# QuPath Extension
The original template can be found at [QuPath Extension Template](https://github.com/qupath/qupath-extension-template). 

## Before using the extension
### Include the following folder and file
Make sure that you have a folder called ".plasmaDetector" in "AppData -> Local -> " that contains the model. 
The torchscript model trained on Zenodo dataset can be located under the [machine learning pipeline](https://github.com/KristianJon/MasterThesis/tree/main/Machine%20Learning%20Pipeline/runs/detect/train/weights). 

### Make sure you have PyTorch installed
This is best achieved by using the extension "Deep Java Library" in QuPath seen in the image below, click on "Manage DJL Engines". 
<img width="281" height="144" alt="image" src="https://github.com/user-attachments/assets/5a36d68c-c04a-4a37-ae68-498fe1df8b1d" />

Afterwards, find the section for "PyTorch" (which is labeled default) and press the button "Check / Download". Everything is good to once you see the green circle and button text "Available".  

<img width="401" height="366" alt="image" src="https://github.com/user-attachments/assets/55c2c0ff-5873-458d-9cf0-8d5522349a3a" />

## Using the extension
Please find the .jar file within the [build folder](https://github.com/KristianJon/MasterThesis/tree/main/QuPath%20Extension/qupath-extension-template/build/libs). The user simply needs to drag this file onto their QuPath-program and restart. Afterwards, the extension should be available through "Extensions -> Plasma Cell Detector". 

<img width="210" height="143" alt="image" src="https://github.com/user-attachments/assets/360eb9b8-bcce-483d-9dd2-719430f19627" />

The user will get an error if: 
- No image is opened
- No annotation is selected

The user should:
- Open an image
- Use the rectangle tool to create an annotation
- Select the annotation
- Press the "predict" button seen below

<img width="993" height="656" alt="image" src="https://github.com/user-attachments/assets/ffbe3e14-a7ce-431a-a393-e9d2ec02b9c6" />


## Results
From the annotation seen above, 260 annotations were created within.  
<img width="317" height="195" alt="image" src="https://github.com/user-attachments/assets/da0a4394-d14f-4f3b-9760-87a0d0a66a7b" />

1. All of the new annotations are created as child annotations of the main rectangle created earlier.  
2. All annotations have the name "YoloDetection", so they can be filtered from the annotation section.  
3. The probability of each annotation is added in the measurement list.  
4. All annotations can be deleted simply by deleting the main parent element (rectangle created earlier). 

At the top right corner, when zoomed in, some of the annotations are:  
<img width="1313" height="673" alt="image" src="https://github.com/user-attachments/assets/e3877472-4cb4-4b3f-8a80-9c9a0da4b0c3" />
