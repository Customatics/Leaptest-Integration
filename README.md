# Leapwork-Integration
This is LEAPWORK plugin for Teamcity

# More Details
LEAPWORK is a mighty automation testing system and now it can be used for running [smoke, functional, acceptance] tests, generating reports and a lot more in Teamcity. You can easily configure integration directly in Teamcity enjoying UI friendly configuration page with easy connection and test suites selection.

# Features:
 - Setup and test LEAPWORK connection in few clicks
 - Run automated tests in your Teamcity build tasks
 - Automatically receive test results
 - Build status based tests results
 - Write tests trace to build output log
 - Smart UI
 
# Installing
- Use maven.
- Command: mvn package 
- Or simply install zip-file from the "target" folder: Copy the zip plugin package into the {TeamCity Data Directory}/plugins directory (Default path: C:\ProgramData\JetBrains\TeamCity\plugins). 
- If you have an earlier version of the plugin in the directory, remove it.
- Alternatively, use the Administration -> Plugins List page to upload the zip plugin package into the directory (do not change the file name of the plugin). Restart the TeamCity server (Services -> Teamcity Server -> Restart). Check the installed plugin version is listed on the Administration -> Plugins List page.

# Update 3.1.0
- For LEAPWORK version 2018.2.262
- Fixed bug when JUnit parser interpreted all flows as "Failed"
- Uses new Leapwork v3 API, API v2 is not supported

# Instruction
1. Add Build-Step "Leapwork Integration" to your project.
2. Enter your LEAPWORK controller hostname or IP-address something like "win10-agent20" or "localhost".
3. Enter your LEAPWORK controller API port, by default it is 9001.
4. Enter time delay in seconds. When schedule is run, plugin will wait this time before trying to get schedule state. If schedule is still running, plugin will wait this time again. By default this value is 5 seconds.
5. Select how plugin should set "Done" status value: to Success or Failed.
6. Press button "Select Schedules" to get a list of all available schedules. Select schedules you want to run.
7. Run your project and get results. Enjoy!

# Troubleshooting
- If you catch an error "No such run [runId]!" after schedule starting, increase time delay parameter.

# Screenshots
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-config.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-overview.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-cases.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-build_log.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-overview-exceptions.png)


