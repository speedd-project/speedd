1. In order to run the CNRS and Feedzai scenarios please do the following: 
-Dowload and extract the content of proton.7z to local directory
-In order to run Proton, you can use the launchProton.bat script. 
Pay attention to the following: Proton.properties file located under /config directory points to the proton definition file which contains the metadata of the application Proton CEP engine is going to be running
Therefore, in order to run the CNRS scenario, the "metadataFileName"  property in the Proton.properties file should point to the location of CNRSnew.json file
For the Feedzai scenario, this property should point at FeedzaiFraud.json file.

Inside the JSON files there is the definition of consumers and producers. 
Currently for Feedzai and CNRS scenarios we are using file consumers and file producers with sample data.
Therefore the file producers in the JSON files should point to CNRSInput.txt (in CNRSnew.json locate the producer and make sure it points to the location of  CNRSInput.txt file)
The same should be done for Feedzai scenario (in FeedzaiFraud.json locate the producer definition, it should point to FeedzaiInput.txt)