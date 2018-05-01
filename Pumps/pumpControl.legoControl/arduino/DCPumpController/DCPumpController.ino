/* 

This is the firmware for an Arduino Uno to drive DC motor pumps through
the Adafruit Motor Shield v2, while under control of our MicroManager
plugins.

© Pedro Almada, UCL, 2015

Commands:
g = get status of all pumps
a = stop all pumps
p = get number of pumps
axy = stop pump xy
sxynnn = for pump xy set speed nnn
rxydttttt - Start pump xy in direction d for ttttt seconds

The commands use this notation:
d = 1 is forward, d = 2 is backwards
x = shield address {1-nShields}
y = motor/pump address {1-nMotorsPerShield}
nnn = speed {000-255}
ttttt = duration {00001-99999}

Our testing with 1 ml syringe tells us the speed will correspond to:
255 - 2.3 microliters/second

*/

//// Imports
#include <EEPROM.h>
// Adafruit Motor Shield libraries
#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include "utility/Adafruit_PWMServoDriver.h"

//// Settings

// Number of shields (MAX: 9)
const int nShields = 4;
// Number of DC motors per shield (MAX on Adafruit Motor Shield v2: 4)
const int nMotorsPerShield = 4;
// Character which signals end of message from PC
char endMessage = '.';

// Misc. initializations
byte currentShield;
byte currentMotor;
int eeAddress = 0;
const int maxMessageLength = 9;
unsigned long startTime = 0;
unsigned long elapsedTime = 0;
unsigned long targetTime = 0;
boolean timeCounter = false;
Adafruit_MotorShield afms[nShields];
Adafruit_DCMotor *motor[nShields][nMotorsPerShield];

// The Arduino should keep track of what the motors have been told to do. 
// We can address each motor's status by accessing the values on motorStatus
// Format is: [shield][motor][speed/state]
// Speed can vary from 0 to 255
// States are: 0 - stop; 1 - forward; 2 - backward.
int motorStatus[nShields][nMotorsPerShield][2] = {{}};

void setup() {
  // Start serial communication and set baud-rate
  Serial.begin(57600);
  Serial.println("Connected!");

  // We now create a Motor Shield object for each shield. Their address 
  // (96-128) is set by soldering a few jumpers.
  // see: https://learn.adafruit.com/adafruit-motor-shield-v2-for-arduino/stacking-shields
  // Note here I'm using decimal values for the address which is simpler
  // but their tutorial uses hex.
  
  unsigned char address = 96;

  // Create array of shield objects
  
  for (int i=0; i<nShields; i++) {
    afms[i] = Adafruit_MotorShield(address);
    address = ++address;
    // Initialize shield objects with the default frequency 1.6KHz
    afms[i].begin();
  }

  // For each shield we then get each of the their motors.
  EEPROM.get(eeAddress, motorStatus);
  for (int i=0; i<nShields; i++) {
    for (int j=0; j<nMotorsPerShield; j++) {
      motor[i][j] = afms[i].getMotor(j+1);
      motor[i][j]->setSpeed(motorStatus[i][j][0]);
    }
  }
}

void loop() {
  char incoming[maxMessageLength];
  
  // If a pump has been started, we stop all pumps after a targetTime amount of time
  if (timeCounter == true) {
    elapsedTime = millis() - startTime;
    if (elapsedTime >= targetTime) {
      for (int i=0; i < nShields; i++) {
           for (int j=0; j < nMotorsPerShield; j++) {
             motorStatus[i][j][1] = 0;
             EEPROM.put(eeAddress, motorStatus);
             motor[i][j]->run(RELEASE);
           }
       }
       elapsedTime = 0;
       targetTime = 0;
       timeCounter = false;
    }
  }
  // If there is a message from the serial port
  if (Serial.available() > 0) {
     
     // Read buffer until end character is reached or maxMessageLength characters are read.
     int lengthOfMessage = Serial.readBytesUntil(endMessage,incoming,maxMessageLength);
     
     // The incoming message will be char array and we're using element 0
     // to determine what is the action to be performed by the pump.
     // The next characters will determine optional parameters such as
     // what pump to start, what speed to set, etc...
     
     // If incoming message is:
     // g = get status of all pumps
     if (incoming[0] == 'g') {
       for (int i=0; i < nShields; i++) {
         for (int j=0; j < nMotorsPerShield; j++) {
           Serial.print("S");
           Serial.print(i + 1);
           Serial.print("M");
           Serial.print(j + 1);
           Serial.print(":");
           Serial.print(motorStatus[i][j][0]);
           Serial.print(",");
           Serial.print(motorStatus[i][j][1]);
           Serial.print(";");
         }
       }
     Serial.println();
     }
     
     // a = stop all pumps
     else if (incoming[0] == 'a' && lengthOfMessage == 1) {
       for (int i=0; i < nShields; i++) {
         for (int j=0; j < nMotorsPerShield; j++) {
           motorStatus[i][j][0] = 255;
           motorStatus[i][j][1] = 0;
           EEPROM.put(eeAddress, motorStatus);
           motor[i][j]->run(RELEASE);
         }
       }
       elapsedTime = 0;
       timeCounter = false;
       Serial.print("Stopped all pumps!");
       Serial.println();
     }
     
     // axy = stop pump xy
     else if (incoming[0] == 'a' && lengthOfMessage != 1) {
       currentShield = incoming[1] - '0' - 1;
       currentMotor  = incoming[2] - '0' - 1;
       motorStatus[currentShield][currentMotor][1] = 0;
       EEPROM.put(eeAddress, motorStatus);
       motor[currentShield][currentMotor]->run(RELEASE);

       elapsedTime = 0;
       timeCounter = false;
       Serial.print("Stopped pump: ");
       Serial.print(incoming[1]);
       Serial.print(",");
       Serial.print(incoming[2]);
       Serial.print("!");
       Serial.println();
     }
     
     //p = get number of pumps
     else if (incoming[0] == 'p') {
      String s = String(nShields, DEC);
      String m = String(nMotorsPerShield, DEC);
      String result = String(s + "." + m);
      Serial.print(result);
      Serial.println();
     }
     
     // sxynnn = for pump xy set speed nnn
     else if (incoming[0] == 's') {
       currentShield = incoming[1] - '0' - 1;
       currentMotor  = incoming[2] - '0' - 1;
       byte a = incoming[3] - '0';
       byte b = incoming[4] - '0';
       byte c = incoming[5] - '0';
       byte currentSpeed = (a*100)+(b*10)+c;
       
       motorStatus[currentShield][currentMotor][0] = currentSpeed;
       EEPROM.put(eeAddress, motorStatus);
       motor[currentShield][currentMotor]->setSpeed(currentSpeed);
       
       Serial.print("Set speed of pump: ");
       Serial.print(incoming[1]);
       Serial.print(",");
       Serial.print(incoming[2]);
       Serial.print(" to ");
       Serial.print(currentSpeed);
       Serial.println();
     }
     
     // rxydttttt - Start pump xy in direction d for ttttt seconds
     // d = 1 is forward, d = 2 is backwards
     else if (incoming[0] == 'r' && lengthOfMessage == maxMessageLength) {
       char targetTimeInput[5] = {incoming[4],incoming[5],incoming[6],incoming[7],incoming[8]};
       targetTime = atol(targetTimeInput)*1000;
       currentShield = incoming[1] - '0' - 1;
       currentMotor  = incoming[2] - '0' - 1;
       motorStatus[currentShield][currentMotor][1] = incoming[3] - '0';
       EEPROM.put(eeAddress, motorStatus);
       
       if (incoming[3] == '1') {
         motor[currentShield][currentMotor]->run(FORWARD);
         startTime = millis();
         elapsedTime = 0;
         timeCounter = true;
         Serial.print("Started pump: ");
         Serial.print(incoming[1]);
         Serial.print(",");
         Serial.print(incoming[2]);
         Serial.print(" in the forward direction.");
         Serial.println();
       }
       else if (incoming[3] == '2') {
         startTime = millis();
         elapsedTime = 0;
         timeCounter = true;
         motor[currentShield][currentMotor]->run(BACKWARD);
         Serial.print("Started pump: ");
         Serial.print(incoming[1]);
         Serial.print(",");
         Serial.print(incoming[2]);
         Serial.print(" in the backward direction.");
         Serial.println();
       };
     }
     
     else {
        Serial.println();
     }
  }
}
