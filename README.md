# Plasma cell detector, an extension to QuPath
This extension adds extra functionality to QuPath by supporting:
- Upload and usage of custom YOLO-models (v11, v12) intended for plasma cell detection by leveraging Deep Java Library (DJL)
- Visualization of predictions in the main QuPath viewer along with color encoding based on prediction confidence
- A 5 x 5 grid view that fetches and displays each prediction along with some of the area surrounding it for contextual awareness
  - Top-left corner of grid view displays the lowest confidence prediction, the remaining 24 cells are its nearest neighbors 
- The option to classify each prediction (class 1: plasma cell, class 2: not plasma cell)

In addition to locating plasma cells, this extension works towards rapid expansion of plasma cell datasets and quick verification/testing of YOLO-models on whole-slide images. 

The original template used to create the extension can be found at [QuPath Extension Template](https://github.com/qupath/qupath-extension-template). 

## Installation 
QuPath enables a very efficient and simple way to use custom extensions. 
1. Download the .jar-file
2. Launch QuPath
3. Drag-and-drop the .jar-file onto the main QuPath-window
   
The extension should be available for use straight away, but a simple restart of QuPath might fix any potential bugs. The extension can be located within "Extensions -> plasma cell detector" in QuPath as seen in the figure below.

<img width="322" height="148" alt="image" src="https://github.com/user-attachments/assets/255c8a87-9084-4032-be44-cba8f47b9ac0" />

### Requirements
The extension currently only supports inference with CPU, but GPU support will be added to the next release. 
Please make sure that you have PyTorch installed, this can be done by:
1. Navigating to "Extensions -> Deep Java Library -> Manage DJL Engines"
2. Locate the section for "PyTorch" and click the button "Check / Download"
3. Ensure that the circle next to "Path" and "Version" changes color from orange to green 

<img width="407" height="369" alt="image" src="https://github.com/user-attachments/assets/c073fc1b-06e0-4a29-beab-a79947bf6f4a" />

## Using the extension
The extension consists of one main window as seen in the figure below. The user needs to open a project and select an image before the extension can be used. 

<img width="805" height="682" alt="image" src="https://github.com/user-attachments/assets/80926031-a782-4cd6-abf8-3ac295aa3b33" />

### Selecting a model and making a prediction
The right side of the main window is used to select the model and to start the inference process. The process is described based on the image below.
1. The user clicks button "Select model" and needs to locate their .torchscript-model from the file explorer window
2. Add the patch size used to train the YOLO-model and click submit
   - If you trained the model on 640 x 640 pixels, simply write "640" and click submit
4. Before the button "Make prediction" can be clicked, the user needs to create at least one rectangle annotation in QuPath where inference should be performed
   - The rectangle annotation needs to be larger than the patch size used during model training
   - The rectangle annotation needs to be selected
   - Multiple rectangle annotations can be created and selected, inference will be performed iteratively 

<img width="188" height="393" alt="image" src="https://github.com/user-attachments/assets/1e5bd9ed-e8b5-4521-ad13-416efb4e5968" />

### Viewing and classifying predictions
Once inference is completed and some predictions exists within the QuPath project, the custom window can be reopened. It will then display all the predictions in the current project that have not been classified. All grid cells are interactive and will be marked with a blue border when selected. The figure below shows a demonstration of this. 

1. The "review" button allows the user to toggle between predictions that have been verified and not
   - The default grid consists of predictions that have not been verified, in order to verify: see point 5
3. The "previous" button will navigate the user back to the previous grid
4. This section will display your current page out of the total number of pages
5. The "next" button will navigate the user on to the next grid view
6. The "verify" button will process the grid view currently being viewed and classify selected grid cells as "plasma cell" and the remaining as "not plasma cell"
   
<img width="560" height="578" alt="image" src="https://github.com/user-attachments/assets/590f4669-fd2a-4364-a7a7-faee38ff0fe3" />
