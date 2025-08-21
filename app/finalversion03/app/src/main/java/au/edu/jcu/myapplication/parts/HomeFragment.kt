package au.edu.jcu.myapplication.parts

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import au.edu.jcu.myapplication.category.Category
import au.edu.jcu.myapplication.categorys.CategoryDao
import au.edu.jcu.myapplication.categorys.CategoryEntity
import au.edu.jcu.myapplication.databasepleaswork.HorzRecipeAdapter
import au.edu.jcu.myapplication.databasepleaswork.RecipeFav
import au.edu.jcu.myapplication.databases.Adapter_Recipe_Class
import au.edu.jcu.myapplication.databases.NewRecipe
import au.edu.jcu.myapplication.databases.Recipe
import au.edu.jcu.myapplication.databases.RecipeDetails
import au.edu.jcu.myapplication.databases.RecipeEntity
import au.edu.jcu.myapplication.databases.RecipesHolder
import au.edu.jcu.myapplication.databinding.FragmentHomeBinding
import au.edu.jcu.myapplication.offlinemode.AppDatabase
import au.edu.jcu.myapplication.offlinemode.RecipeRepository
import au.edu.jcu.myapplication.ui.applyAppSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Collections

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var horzAdapter: HorzRecipeAdapter

    //single db/dao refs
    private val appDb by lazy { AppDatabase.getInstance(requireContext()) }
    private val categoryDao by lazy { appDb.categoryDao() }


    private val categorySet = mutableSetOf<String>()



    //firebase
    val categoriesRef  = FirebaseDatabase.getInstance().getReference("Categories")


    //sync to online database from offline database
    private fun String.isLocalId() = startsWith("local_")
    private fun String.isLocalUri() = startsWith("content://") || startsWith("file://")


    //preventing multiple syncs
    private var isSyncing = false
    private val syncingIds = Collections.synchronizedSet(mutableSetOf<String>())



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //settings
        applyAppSettings(binding.root)

        //open activity new recipe
        binding.MoreRecipebutton.setOnClickListener {
            openNewRecipe()

        }

        binding.syncNowButton.setOnClickListener {
            syncPendingRecipes()
        }

        //disabled scrolling on the favourites list so the whole page can scroll
        binding.favouritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }


        horzAdapter = HorzRecipeAdapter().also { adapter ->
            binding.favouritesRecyclerView.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.favouritesRecyclerView.adapter = adapter
            adapter.setOnItemClickListener { recipe ->
                startActivity(Intent(requireContext(), RecipesHolder::class.java).putExtra("recipe", recipe))
            }
        }

        if (isConnectedToInternet(requireContext())) {
            loadFavoriteRecipesFromFirebase()
            loadCategoriesCombined()
        } else {
            loadOfflineRecipes()
            Toast.makeText(requireContext(), "Offline mode: Showing saved recipes",
                Toast.LENGTH_SHORT).show()
            showCategoriesFromRoom()
        }



        //category search
        binding.searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text?.toString().orEmpty().trim()
                if (query.isNotEmpty()) {
                    startActivity(
                        Intent(requireContext(), RecipeDetails::class.java)
                            .putExtra("type", "search")
                            .putExtra("query", query)
                    )
                }
                true
            } else false
        }

        //category box filter
        binding.searchEditText.addTextChangedListener { text ->
            val q = text?.toString()?.trim().orEmpty().lowercase()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val all = categoryDao.getAll().map { it.name }.sorted()
                val filtered = if (q.isEmpty()) all else all.filter { it.lowercase().contains(q) }
                withContext(Dispatchers.Main) {
                    displayCategories(filtered)
                }
            }
        }


    }

    override fun onResume() {
        super.onResume()
        applyAppSettings(binding.root)

        if (isConnectedToInternet(requireContext())) {
            syncPendingRecipes()
            //also refresh favorites
            loadFavoriteRecipesFromFirebase()
        }
    }

    private fun openNewRecipe() {
        val intent = Intent(requireContext(), NewRecipe::class.java)
        startActivity(intent)
    }




    //load offline recipes
    private fun loadOfflineRecipes() {
        val repository = RecipeRepository(requireContext())

        CoroutineScope(Dispatchers.IO).launch {
            val offlineEntities = repository.getAllRecipes()
            val offlineRecipes = offlineEntities.map {
                Recipe(
                    id = it.id,
                    name = it.name,
                    ingredient = it.ingredient,
                    steps = it.steps,
                    category = it.category,
                    image = it.image,
                    authorId = it.authorId
                )
            }

            withContext(Dispatchers.Main) {

                horzAdapter.setRecipeList(offlineRecipes)
                horzAdapter.setOnItemClickListener { recipe ->
                    val intent = Intent(requireContext(), RecipesHolder::class.java)
                    intent.putExtra("recipe", recipe)
                    startActivity(intent)
                }
                binding.favouritesRecyclerView.adapter = horzAdapter
            }
        }
    }

    private fun loadCategoriesCombined() {
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        val firebaseNames = snapshot.children.mapNotNull {
                            it.getValue(Category::class.java)?.name
                        }


                        val cleanedDistinct = firebaseNames
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinctBy { it.lowercase() }

                        //insert to Room
                        categoryDao.insertAll(cleanedDistinct.map { CategoryEntity(it) })
                    }
                    //render from room
                    renderCategoriesFromRoom()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                //firebase failed
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    renderCategoriesFromRoom()
                }
            }
        })
    }



    private suspend fun renderCategoriesFromRoom() {
        val all = categoryDao.getAll().map { it.name }.sorted()
        withContext(Dispatchers.Main) {
            categorySet.clear()
            categorySet.addAll(all.map { it.lowercase() })
            displayCategories(all)
        }
    }


    private fun showCategoriesFromRoom() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { renderCategoriesFromRoom() }
    }

    //add category
    private fun addCategorySmart(nameRaw: String) {
        val name = nameRaw.trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        if (categorySet.contains(name.lowercase())) {
            Toast.makeText(requireContext(), "Category already exists.", Toast.LENGTH_SHORT).show()
            return
        }

        //always add to Room first so UI shows it immediately
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            categoryDao.insertAll(listOf(CategoryEntity(name)))
            val all = categoryDao.getAll().map { it.name }.sorted()
            withContext(Dispatchers.Main) { displayCategories(all) }
        }

        //if online, also push to Firebase now
        if (isConnectedToInternet(requireContext())) {
            val key = name.lowercase()
            categoriesRef.child(key).setValue(Category(name))
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to add category online. Saved locally.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Saved category locally (offline).", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showAddCategoryDialog() {
        val input = EditText(requireContext()).apply { hint = "e.g., Dessert" }
        AlertDialog.Builder(requireContext())
            .setTitle("New Category")
            .setMessage("Enter the name of the new category")
            .setView(input)
            .setPositiveButton("Add") { dialog, _ ->
                val newCategory = input.text.toString().trim()
                when {
                    newCategory.isEmpty() -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Invalid Name")
                            .setMessage("Name cannot be empty.")
                            .setPositiveButton("OK") { d, _ -> d.dismiss() }
                            .show()
                    }
                    categorySet.any { it.equals(newCategory, ignoreCase = true) } -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Duplicate Category")
                            .setMessage("The category \"$newCategory\" already exists.")
                            .setPositiveButton("OK") { d, _ -> d.dismiss() }
                            .show()
                    }
                    else -> {
                        addCategorySmart(newCategory)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }

    ///////////////////////////////////////////////////////

    //category Display
    private fun displayCategories(categoryNames: List<String>) {
        val sorted = categoryNames.sorted()
        binding.categoryContainer.removeAllViews()

        for (category in sorted) {
            val button = Button(requireContext()).apply {
                text = category
                textSize = 14f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(16, 8, 16, 8)
                setBackgroundColor(Color.parseColor("#DDDDDD"))
                setOnClickListener {
                    //open RecipeDetails for this category
                    val intent = Intent(requireContext(), RecipeDetails::class.java)
                        .putExtra("type", "category")
                        .putExtra("category", category)
                    startActivity(intent)

                }
            }
            binding.categoryContainer.addView(button)
        }
        addCreateCategoryButton()
    }


    private fun addCreateCategoryButton() {
        val addButton = Button(requireContext()).apply {
            text = "Add New Category"
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.LTGRAY)
            setOnClickListener { showAddCategoryDialog() }
        }
        binding.categoryContainer.addView(addButton)
    }





    ///////////////////
    //Favourites functions
    private fun loadFavoriteRecipesFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("HomeFragment", "Loading favorites for userId: $userId")
        Log.d("HomeFragment", "Favorites path: /Favorites/$userId")
        Log.d("HomeFragment", "Recipes path: /Recipes")

        val favRef = FirebaseDatabase.getInstance()
            .getReference("Favorites")
            .child(userId)

        favRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(favSnapshot: DataSnapshot) {
                Log.d("HomeFragment",
                    "Favorites snapshot exists: ${favSnapshot.exists()} | children: ${favSnapshot.childrenCount}")

                val favoriteIds = mutableListOf<String>()

                for (child in favSnapshot.children) {
                    Log.d("HomeFragment",
                        "Favorite raw child: key=${child.key}, value=${child.value}")

                    child.key?.let { key ->
                        val valIsBoolOr1 = (child.getValue(Boolean::class.java) == true) ||
                                (child.getValue(Int::class.java) == 1)
                        if (valIsBoolOr1) favoriteIds.add(key)
                        Log.d("HomeFragment", "Added favorite key from bool/int: $key")
                    }

                    //value is an object with id
                    child.getValue(RecipeFav::class.java)?.id?.let { favoriteIds.add(it.toString()) }
                    child.getValue(Recipe::class.java)?.id?.let { favoriteIds.add(it.toString()) }
                }

                val ids = favoriteIds.distinct()
                if (ids.isEmpty()) {
                    horzAdapter.setRecipeList(emptyList())
                    binding.favouritesRecyclerView.visibility = View.GONE
                    binding.noFavouritesTextView.visibility = View.VISIBLE
                    return
                }

                // now fetch recipes
                val recipeRef = FirebaseDatabase.getInstance().getReference("Recipes")
                recipeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val recipes = mutableListOf<Recipe>()
                        for (snap in dataSnapshot.children) {
                            val recipe = snap.getValue(Recipe::class.java)
                            val recId = recipe?.id ?: snap.key
                            Log.d("HomeFragment", "Checking recipe: id=$recId name=${recipe?.name}")

                            if (recId != null && ids.contains(recId)) {
                                val withId = if (recipe?.id == null) {
                                    recipe!!.copy(id = recId)
                                } else {
                                    recipe
                                }
                                recipes.add(withId)
                                Log.d("HomeFragment", "Added recipe to favorites list: ${withId.name}")
                            }
                        }

                        horzAdapter.setRecipeList(recipes)

                        val has = recipes.isNotEmpty()
                        binding.favouritesRecyclerView.visibility = if (has) View.VISIBLE else View.GONE
                        binding.noFavouritesTextView.visibility = if (has) View.GONE else View.VISIBLE
                        Log.d("HomeFragment", "Final favorite recipes count: ${recipes.size}")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HomeFragment", "Error loading recipes: ${error.message}")
                        horzAdapter.setRecipeList(emptyList())
                        binding.favouritesRecyclerView.visibility = View.GONE
                        binding.noFavouritesTextView.visibility = View.VISIBLE
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error loading favorites: ${error.message}")
                horzAdapter.setRecipeList(emptyList())
                binding.favouritesRecyclerView.visibility = View.GONE
                binding.noFavouritesTextView.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed to load favorites",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


//checking internet connection
    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }


    ///////////////////

    //sync database
    private fun syncPendingRecipes() {
        if (!isConnectedToInternet(requireContext())) {
            Log.d("Sync", "Skip: offline")

            return

    }
        if (isSyncing) {
            Toast.makeText(
                requireContext(),
                "Sync already running…",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isSyncing = true

            binding.syncNowButton?.isEnabled = false

            val appDb = AppDatabase.getInstance(requireContext())
        val recipeDao = appDb.recipeDaoRoom()
        val categoriesRef = FirebaseDatabase.getInstance().getReference("Categories")
        val recipesRef = FirebaseDatabase.getInstance().getReference("Recipes")
        val storage = FirebaseStorage.getInstance()

        ////////////////////////////////////

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            Log.d("Sync", "Start — loading local pending recipes")
            val localRecipes = recipeDao.getAllLocal()
            Log.d("Sync", "Found ${localRecipes.size} local recipes (id like local_%)")


            if (localRecipes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Nothing to sync.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

///////////////////////////////////
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    withContext(Dispatchers.Main) { Log.d("Sync", "Signing in anonymously…") }
                    auth.signInAnonymously().await()
                }
            } catch (e: Exception) {
                Log.e("Sync", "Auth failed, aborting sync: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Sync failed (auth).", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            var successCount = 0
            for (entity in localRecipes) {
                try {
                    Log.d("Sync", "Syncing local id=${entity.id}, name=${entity.name}")

                    //upload image if it’s a local Uri; if it fails continue without image
                    var imageUrl = entity.image ?: ""
                    if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                        try {
                            val newIdForImage = recipesRef.push().key ?: System.currentTimeMillis().toString()
                            val targetRef = storage.getReference("images/$newIdForImage.jpg")
                            val uri = Uri.parse(imageUrl)
                            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                                targetRef.putStream(input).await()
                            }
                            imageUrl = targetRef.downloadUrl.await().toString()
                            Log.d("Sync", "Uploaded image for ${entity.id} -> $imageUrl")
                        } catch (ie: Exception) {
                            Log.e("Sync", "Image upload failed for ${entity.id}: ${ie.message}", ie)
                            // fallback: push recipe without image
                            imageUrl = ""
                        }
                    }

                    //create cloud id and push recipe
                    val cloudId = recipesRef.push().key ?: System.currentTimeMillis().toString()
                    val recipe = Recipe(
                        id = cloudId,
                        name = entity.name,
                        ingredient = entity.ingredient,
                        steps = entity.steps,
                        category = entity.category,
                        image = imageUrl,
                        authorId = entity.authorId
                    ).apply {
                        authorId_name_lc = "${entity.authorId}_${entity.name.lowercase()}"
                    }

                    // push recipe
                    recipesRef.child(cloudId).setValue(recipe).await()
                    Log.d("Sync", "Pushed recipe ${entity.name} as $cloudId")

                    //ensure category exists in cloud
                    val catKey = entity.category.trim().lowercase()
                    categoriesRef.child(catKey).setValue(Category(name = entity.category)).await()

                    //replace local id with cloud id so it shows online next time
                    val newEntity = entity.copy(id = cloudId, image = imageUrl)
                    recipeDao.insert(newEntity)
                    recipeDao.deleteById(entity.id)
                    Log.d("Sync", "Replaced local ${entity.id} with cloud $cloudId in Room")

                    successCount++

                } catch (e: Exception) {
                    Log.e("Sync", "Failed to sync ${entity.id}: ${e.message}", e)
                    // do not delete local
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(),
                    "Sync complete: $successCount/${localRecipes.size} recipes uploaded.",
                    Toast.LENGTH_LONG
                ).show()

                isSyncing = false
                binding.syncNowButton?.isEnabled = true

                //optional refresh UI
                loadFavoriteRecipesFromFirebase()
                showCategoriesFromRoom()
            }
        }
    }

    ///////////////////////////


}



