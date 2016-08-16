#!/bin/bash
#
EMULATOR_DIR="/home/andreas/opt/android-sdk-linux/tools"
PROJECT_DIR="/home/andreas/IdeaProjects/SkyLinesTracker"
TEST_DIR="/home/andreas/IdeaProjects/SkyLinesTracker/Tests"
IP=$(hostname -I | awk '{print $1}')
INT=2
KEY="ABCD1234"

cd ${TEST_DIR}/scripts
rm -rf sim-test-*.out
rm -rf rcv-test-*.out
pkill -f UDP-Receiver.jar

trap "pkill -f UDP-Receiver.jar; exit" INT TERM EXIT

${EMULATOR_DIR}/emulator -avd Device -netspeed full -netdelay none -no-boot-anim &

sleep 15
python preference_file.py ${KEY} ${INT}  false  false ${IP} true 2048

##adb -s emulator-5554 uninstall ch.luethi.skylinestracker
adb -s emulator-5554 push ch.luethi.skylinestracker_preferences.xml /data/data/ch.luethi.skylinestracker/shared_prefs/
adb -s emulator-5554 install -r  ${PROJECT_DIR}/out/SkyLinesTracker.apk
sleep 15

adb -s emulator-5554 shell am start -W -n ch.luethi.skylinestracker/ch.luethi.skylinestracker.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -e ISTESTING true -e TESTING_IP ${IP}
adb -s emulator-5554 shell svc data enable
adb -s emulator-5554 shell ls -l  /data/data/ch.luethi.skylinestracker/shared_prefs/ch.luethi.skylinestracker_preferences.xml

sleep 15

echo "### $(date +"%T") GPS simmluation, LiveTracking NOT checked"
java -jar ${TEST_DIR}/UDP-Receiver.jar -br > rcv-test-00.out &
python gps_simulator.py 1200 ${KEY} > sim-test.out &
sleep 60
pkill -f UDP-Receiver.jar

echo "### $(date +"%T") GPS simmluation, LiveTracking checked"
sh clickLiveTracking.sh
java -jar ${TEST_DIR}/UDP-Receiver.jar -br > rcv-test-01.out &
sleep 60
pkill -f UDP-Receiver.jar

echo "### $(date +"%T") GPS simmluation, LiveTracking NOT checked again"
sh clickLiveTracking.sh
java -jar ${TEST_DIR}/UDP-Receiver.jar -br > rcv-test-02.out &
sleep 60
pkill -f UDP-Receiver.jar


sleep 15
pkill -f gps_simulator.py

echo "#### $(date +"%T") Shuting down everting....................."
adb -s emulator-5554 shell am force-stop ch.luethi.skylinestracker
adb -s emulator-5554 emu kill
pkill -f qemu-system-x86_64
exit