# Leaptest-Integration(Windows)
This is Leaptest plugin for Teamcity

# More Details
Leaptest is a mighty automation testing system and now it can be used for running [smoke, functional, acceptance] tests, generating reports and a lot more in Teamcity. You can easily configure integration directly in Teamcity enjoying UI friendly configuration page with easy connection and test suites selection. 

# Features:
 - Setup and test Leaptest connection in few clicks
 - Run automated tests in your Teamcity build tasks
 - Automatically receive test results
 - Build status based tests results
 - Write tests trace to build output log
 - Smart UI
 
# Installing
- Use maven 3.0.1.
- Command: mvn package 
- Or simply install zip-file from the "target" folder: Administration -> Plugin List -> Upload Plugin Zip -> Choose that zip-file

# Instruction
1. Add Build-Step "Leaptest for TeamCity" to your project.
2. Enter your Leaptest server address something like "http://win10-agent2:9000" or "http://localhost:9000".
3. Enter time delay in seconds. When schedule is run, plugin will wait this time before trying to get schedule state. If schedule is still running, plugin will wait this time again. By default this value is 1 second.
5. Select how plugin should set "Done" status value: to Success or Failed.
6. Press button "Select Schedules" to get a list of all available schedules grouped by projects. Select schedules you want to run.
4. Run your project and get results. Enjoy!

# Screenshots
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-config.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-overview.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-cases.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-build_log.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/tc-report-overview-exceptions.png)


