@echo off
echo Running BQJavaAPI with real BigQuery and mocked API...
gradlew.bat --no-daemon bootRun > app-output.log 2>&1
echo Application output is being saved to app-output.log
echo You can check the progress by opening app-output.log in another window
