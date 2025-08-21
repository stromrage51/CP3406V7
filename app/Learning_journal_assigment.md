# Learning Journal assigment

## Description: What happened during the project? 
This project focused on developing a recipe app with both offline and online features, replacing an outdated UI with improved functionality. I began by merging my paper prototype and previous UI, iterating designs before creating the initial XML files. Drawing inspiration from existing apps, I added favourites and navigation elements for easier feature integration.

I prioritized connecting all pages and implemented user authentication, including custom loading, sign-in, and sign-up screens. The settings page initially faced issues with changes not applying globally, but I resolved this by researching broader application methods.

Building the online database was challenging due to differences from reference material, resulting in extensive code rewriting. After encountering persistent issues with the online favourites system, I temporarily set it aside. Gallery image uploads replaced an unstable camera function, and I postponed the checkbox feature due to design uncertainties.

To ensure flexibility, I created a separate offline database project, enabling system comparison and easier future integration. This process revealed that Firebase blocked the online favourites system, though the offline version worked. Upgrading `AppDatabase` caused migration problems, fixed by implementing proper migration logic.

Once both databases were functional, I merged them, using ‘ConnectivityManager` to toggle between modes and resolving flow conflicts with conditional statements. After cleaning up the code and adding minor features like a timer, I backed up to GitHub, narrowly avoiding data loss.
Near the deadline, offline uploads malfunctioned, and online reconnections failed, but these were resolved under time constraints, leading me to drop unfinished features such as the checkbox and online favourites. Progress was slow due to external commitments, but despite setbacks, I successfully integrated offline and online databases.


## Problems: What were the problems?
Several challenges arose in the absence of the assignment, which can be classified into two main categories: code-related issues and time management difficulties.

Among the code-related problems, the primary issue was that the 'favourite' RecyclerView functioned offline but failed to operate online due to Firebase restrictions. Additionally, offline mode encountered failures near completion because of complications with uploads and placeholders. A problem that spanned both coding and time constraints involved the camera feature, which repeatedly crashed when accessed, and I was unable to resolve the issue within the available timeframe. Another significant coding obstacle was database migration; although I believed I understood the concept, my lack of practical knowledge hindered successful implementation.
Regarding time-related issues, a considerable amount of time was devoted to developing the favourite system, as it was a core feature for the application. The root cause of certain problems was not identified until much later, resulting in inefficient use of time and delays in implementing solutions. Unfortunately, I was also unable to develop the checkbox system, despite its importance, due to the extensive errors that required resolution. The greatest challenge, however, was managing my schedule—balancing two jobs, one of which demanded up to 12 hours per shift, left me with very limited time to dedicate to the app. As a result, I often felt frustrated about not being able to make the desired progress.


# Videos I used:
Idea for online part of recipe app:
    https://www.youtube.com/watch?v=4xBT7gESjEU 

For auto sizing the text: 
    https://www.youtube.com/watch?v=9LTFmn_4Okk 

For changing the colour of the background: 
    https://www.youtube.com/watch?v=DpyJZ-f6xVg 

For Process bar:
    https://www.youtube.com/watch?v=SaTx-gLLxWQ

For offline database room: 
    https://www.youtube.com/watch?v=bOd3wO0uFr8

For idea to how to connect two databases: 
    https://www.youtube.com/watch?v=UNR1aphXtjI

For online and offline difference:
    https://www.youtube.com/watch?v=TzV0oCRDNfM  


# Evaluation 
Several aspects of the project were successful: user interface improvements from paper prototyping, smooth integration of offline and online databases, effective use of “ConnectivityManager” for mode switching, a useful recipe timer, and a reliable offline favourites system. However, I struggled with time management on the online favourites feature, had to drop features like the checkbox and camera, encountered migration issues that slowed progress, and dealt with last-minute offline mode problems.

# Analysis
Positive results were achieved when I followed a structured plan, as demonstrated during the integration of offline and online databases. Utilizing proven examples for adaptation and ensuring backups prior to implementing high-risk changes contributed to overall success. Equally, challenges arose when unnecessary attention was given to a selection of specific issues such as persistent troubleshooting of the online favourites system. Limited knowledge with Room migration processes and the lack of detailed implementation plans for certain features, including the checkbox functionality, led to avoidable delays and setbacks.

# Conclusion 
This project provided valuable insights into several core areas. It facilitated the successful integration of offline and online databases within an Android application and highlighted the necessity of maintaining separation between systems during development to prevent potential conflicts. Additionally, it reinforced the significance of backups and version control, while enhancing my abilities in resolving migration challenges. The experience also revealed certain limitations of Firebase that must be considered when designing and implementing features.

# Content Insight
The combination of online and offline synchronization with category-based recipe browsing is not commonly found in existing apps, which may offer unique advantages for this project. Offline functionality is particularly important for users in areas with limited internet connection. Additionally, features such as the checkbox system require thorough and precise design planning prior to development to reduce the likelihood of being discontinued. 

# Action Plan — What I Would Do Differently Next Time
If I repeated the project, I'd prioritise planning feature designs before coding, minimise time on blocked features, start database migration early, schedule consistent work sessions, and use mock data with offline-first testing for quicker and more reliable results.

