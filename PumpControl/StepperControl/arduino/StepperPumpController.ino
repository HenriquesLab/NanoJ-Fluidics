/* 

This is the firmware for an Arduino Uno to drive DC motor pumps through
the Adafruit Motor Shield v2, while under control of our MicroManager
plugins.

© Pedro Almada, UCL, 2015

Commands:
g = get status
a = stop pump
snnn = for pump xy set speed nnn
rdtttttttt - Start pump xy in direction d for ttttttt steps

The commands use this notation:
d = 1 is forward, d = 2 is backwards
nnn = speed {000-maximumSpeed}
ttttttt = steps {0000001-9999999}



*/

//// Imports
#include <EEPROM.h>
// Adafruit Motor Shield libraries
#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include <AccelStepper.h>
#include "utility/Adafruit_PWMServoDriver.h"


// How many steps can the motor take per full revolution?
const uint16_t resolution = 3200;
const uint16_t maximumSpeed = 400;


// Character which signals end of message from PC
char endMessage = '.';

// Misc. initializations
int eeAddress = 0;
int motorStatus = 0;
const uint32_t acceleration = resolution*resolution;
const int maxMessageLength = 9;
Adafruit_MotorShield shield = Adafruit_MotorShield();
Adafruit_StepperMotor *motor = shield.getStepper(resolution, 1);

void forward() {  
  motor->onestep(FORWARD, MICROSTEP);
}
void backward() {  
  motor->onestep(BACKWARD, MICROSTEP);
}

AccelStepper stepper(forward, backward);

void setup() {
  // Start serial communication and set baud-rate
  Serial.begin(57600);
  Serial.println("Connected!");

  shield.begin();

  stepper.setMaxSpeed(resolution);
  stepper.setAcceleration(acceleration);
  
  motor->release();
}

void loop() {
  stepper.run();
  char incoming[maxMessageLength];

  // If there is a message from the serial port
  if (Serial.available() > 0) {
     stepper.run();
     
     // Read buffer until end character is reached or maxMessageLength characters are read.
     int lengthOfMessage = Serial.readBytesUntil(endMessage,incoming,maxMessageLength);
     
     stepper.run();
     
     // The incoming message will be char array and we're using element 0
     // to determine what is the action to be performed by the pump.
     // The next characters will determine optional parameters such as
     // what pump to start, what speed to set, etc...

    // get status of pump
    if (incoming[0] == 'g') {
     Serial.print(motorStatus);
     Serial.println();
     }
     
     // a = stop pump
     else if (incoming[0] == 'a') {
       stepper.stop();
       //stepper.moveTo(stepper.currentPosition());
       motorStatus = 0;
       EEPROM.put(eeAddress, motorStatus);
       Serial.print("Stopped pump!");
       Serial.println();
       
     }
     
     
     // snnn = set speed nnn
     else if (incoming[0] == 's' & lengthOfMessage == 4) {
       char s[7];
        for (int i = 1; i < 4; i++) {
          s[i-1] = incoming[i];
        }
       uint16_t currentSpeed = atol(s);

       if (currentSpeed > maximumSpeed) {
        currentSpeed = maximumSpeed;
       }
       
       stepper.setMaxSpeed(currentSpeed);
       stepper.setAcceleration(currentSpeed*currentSpeed);
       
       Serial.print("Set speed of pump to: ");
       Serial.print(currentSpeed);
       Serial.println();
     }


     
     // rdttttttt - Start pump in direction d for 9999999 steps
     // d = 1 is forward, d = 2 is backwards
     else if (incoming[0] == 'r') {
        char ta[7];
        for (int i = 2; i < 9; i++) {
          ta[i-2] = incoming[i];
        }
        uint32_t target = atol(ta);
       
       if (incoming[1] == '1') {
         Serial.print("Started pump in the forward direction for ");
         Serial.print(target);
         Serial.print(" steps.");
         Serial.println();
         stepper.move(target);
         stepper.run();
         motorStatus = 1;
         EEPROM.put(eeAddress, motorStatus);
       }
       
       else if (incoming[1] == '2') {
         Serial.print("Started pump in the backward direction for ");
         Serial.print(target);
         Serial.print(" steps.");
         Serial.println();
         stepper.move(-target);
         motorStatus = 2;
         EEPROM.put(eeAddress, motorStatus);
       };
     }
     
     else {
       stepper.run();
        Serial.print("Yes?");
        Serial.println();
     }
  }  

  
}

