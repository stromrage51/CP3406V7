#Learning Journal 

##Description: What happened during the project? 
This project focused on developing a recipe app with both offline and online features, replacing an outdated UI with improved functionality. I began by merging my paper prototype and previous UI, iterating designs before creating the initial XML files. Drawing inspiration from existing apps, I added favourites and navigation elements for easier feature integration.

I prioritized connecting all pages and implemented user authentication, including custom loading, sign-in, and sign-up screens. The settings page initially faced issues with changes not applying globally, but I resolved this by researching broader application methods.

Building the online database was challenging due to differences from reference material, resulting in extensive code rewriting. After encountering persistent issues with the online favourites system, I temporarily set it aside. Gallery image uploads replaced an unstable camera function, and I postponed the checkbox feature due to design uncertainties.

To ensure flexibility, I created a separate offline database project, enabling system comparison and easier future integration. This process revealed that Firebase blocked the online favourites system, though the offline version worked. Upgrading `AppDatabase` caused migration problems, fixed by implementing proper migration logic.

Once both databases were functional, I merged them, using ‘ConnectivityManager` to toggle between modes and resolving flow conflicts with conditional statements. After cleaning up the code and adding minor features like a timer, I backed up to GitHub, narrowly avoiding data loss.
Near the deadline, offline uploads malfunctioned, and online reconnections failed, but these were resolved under time constraints, leading me to drop unfinished features such as the checkbox and online favourites. Progress was slow due to external commitments, but despite setbacks, I successfully integrated offline and online databases.


##Problems: What were the problems?

