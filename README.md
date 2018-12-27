# RIOT: a Novel Stochastic Method for Rapidly Configuring Cloud-Based Workflow

## Files Organization
- LICENSE: This project is under MIT License
- build.xml: For maven build
- plots: Source code for concluding / ploting results, implementing in Python
- vmlb: Source code and models for RIOT, implementing in Java
- results: Saved results

## Building the project
- install Apache Ant. See [here](http://ant.apache.org/manualdownload.cgi) 
- build the java project  
```
cd vmlb
ant build
```
- run corresponding jar file

## Executing jar files  
- Executing EMSC algorithms  
```ant ExpEMSC ARGS1 ARGS2 ARGS3```  
ARGS1 = number of repeats  
ARGS2 = model name/small/all  
ARGS3 = algorithm. nsgaii/spea2/moead

- Executing RIOT algorithms  
```ant ExpRIOT ARGS1 ARGS2```  
ARGS1 = number of repeats  
ARGS2 = model name/small/all

- Changing internal parameters?!
See src in package edu.ncsu.experiments
