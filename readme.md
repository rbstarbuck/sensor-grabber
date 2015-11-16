Application for recording accelerometer and gyroscope data from Android smartwatch on bluetooth-connected mobile phone.

This application was developed to assist in research efforts at DPM Group relating to action recognition using wrist-mounted inertial measurement unit (IMU) data. This software is provided as-is and no guarentees are made regarding its efficacy on devices other than Moto 360 smartwatch.

Data are stored in a CSV file within the Documents folder of the paired phone. Each recording session is timestamped--timestamps are included in file names, and time readings in first column of CSV are in milliseconds from the start time.

The source code can be found in:
 - <code>./mobile/</code>: phone program
 - <code>./wear/</code>: smartwatch program
 - <code>./shared/</code>: shared .lib which defines common data structures and buffers
