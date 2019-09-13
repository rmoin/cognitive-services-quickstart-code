import com.microsoft.azure.cognitiveservices.vision.faceapi.*;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.*;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * This quickstart contains:
 *  - Detect Faces: detect a face or faces in an image and URL
 *  - Find Similar: find face similar to the single-faced image in the group image
 * 
 * Prerequisites:
 * - Create a lib folder in the root directory of your project, then add the jars from dependencies.txt
 * - Download the FaceAPI library jar and add to your lib folder.
 * - Replace the "myRegion" variable in the authenticate section with your region. 
 *   The "westus" is used, otherwise, as the default.
 * 
 * To compile and run, enter the following at a command prompt:
 *   javac FaceQuickstart.java -cp .;lib\*
 *   java -cp .;lib\* FaceQuickstart
 *
 * Note If you run this sample with JRE 9+, you may encounter the following issue: 
 * https://github.com/Azure/autorest-clientruntime-for-java/issues/569 which results in the following output:
 * WARNING: An illegal reflective access operation has occurred ... (plus several more warnings)
 *
 * This should not prevent the sample from running correctly, so this can be ignored.
 * 
 * References:
 *  - Face Documentation: https://docs.microsoft.com/en-us/azure/cognitive-services/face/
 *  - Face Java SDK: https://docs.microsoft.com/en-us/java/api/overview/azure/cognitiveservices/client/faceapi?view=azure-java-stable
 *  - API Reference: https://docs.microsoft.com/en-us/azure/cognitive-services/face/apireference
 */

public class FaceQuickstart {

    public static void main(String[] args) {

        // For Detect Faces and Find Similar Faces examples
        // This image should have a single face.
        final String singleFaceUrl = "https://www.biography.com/.image/t_share/MTQ1MzAyNzYzOTgxNTE0NTEz/john-f-kennedy---mini-biography.jpg";
        final String singleImageName = singleFaceUrl.substring( singleFaceUrl.lastIndexOf('/')+1, singleFaceUrl.length() );
        // This image should have several faces. At least one should be similar to the face in singleFaceImage.
        final String  groupFacesUrl = "http://www.historyplace.com/kennedy/president-family-portrait-closeup.jpg";
        final String groupImageName = groupFacesUrl.substring( groupFacesUrl.lastIndexOf('/')+1, groupFacesUrl.length() );

        // For Identify Faces example
        final String IMAGE_BASE_URL = "https://csdx.blob.core.windows.net/resources/Face/Images/";

        /**
         * Authenticate
         */
        // Add FACE_SUBSCRIPTION_KEY to your environment variables with your key as the value.
        final String key = System.getenv("FACE_SUBSCRIPTION_KEY");

        // Add your region of your Face subscription, for example 'westus', 'eastus', etc.
        // List of Azure Regions: https://docs.microsoft.com/en-us/java/api/com.microsoft.azure.cognitiveservices.vision.faceapi.models.azureregions?view=azure-java-stable
        final AzureRegions myRegion = AzureRegions.WESTUS;

        // Create Face client
        FaceAPI client = FaceAPIManager.authenticate(myRegion, key);
        /**
         * END - Authenticate
         */

        System.out.println("============== Detect Face ==============");
        // Detect the face in a single-faced image. Returns a list of UUIDs and prints them.
        List<UUID> singleFaceID = detectFaces(client, singleFaceUrl, singleImageName);
        // Detect the faces in a group image. Returns a list of UUIDs and prints them.
        List<UUID> groupFaceIDs = detectFaces(client, groupFacesUrl, groupImageName);

        System.out.println("============== Find Similar ==============");
        // Finds a similar face in group image. Returns a list of UUIDs and prints them.
        findSimilar(client, singleFaceID, groupFaceIDs, groupImageName);

        System.out.println("============== Identify ==============");
        // Groups similar photos of a person, then uses that group 
        // to recognize the person in another photo.
        identifyFaces(client, IMAGE_BASE_URL);
    }
    /**
     * END - Main
     */

    /**
     * Detect Face
     * Detects the face(s) in an image URL.
     */
    public static List<UUID> detectFaces(FaceAPI client, String imageURL, String imageName) {
        // Create face IDs list
        List<DetectedFace> facesList = client.faces().detectWithUrl(imageURL, new DetectWithUrlOptionalParameter().withReturnFaceId(true));
        System.out.println("Detected face ID(s) from URL image: " + imageName  + " :");
        // Get face(s) UUID(s)
        List<UUID> faceUuids = new ArrayList<>();
        for (DetectedFace face : facesList) {
            faceUuids.add(face.faceId());
            System.out.println(face.faceId()); 
        }
        System.out.println();

        return faceUuids;
    }
    /**
     * END - Detect Face
     */

    /**
     * Find Similar
     * Finds a similar face in another image with 2 lists of face IDs. 
     * Returns the IDs of those that are similar.
     */
    public static List<UUID> findSimilar(FaceAPI client, List<UUID> singleFaceList, List<UUID> groupFacesList, String groupImageName) {
        // With our list of the single-faced image ID and the list of group IDs, check if any similar faces.
        List<SimilarFace> listSimilars = client.faces().findSimilar(singleFaceList.get(0),
                                             new FindSimilarOptionalParameter().withFaceIds(groupFacesList));
        // Display the similar faces found
        System.out.println();
        System.out.println("Similar faces found in group photo " + groupImageName + " are:");
        // Create a list of UUIDs to hold the similar faces found
        List<UUID> similarUuids = new ArrayList<>();
        for (SimilarFace face : listSimilars) {
            similarUuids.add(face.faceId());
            System.out.println("Face ID: " + face.faceId());
            // Get and print the level of certainty that there is a match
            // Confidence range is 0.0 to 1.0. Closer to 1.0 is more confident
            System.out.println("Confidence: " + face.confidence());
        }
        System.out.println();

        return similarUuids;
    }
    /**
     * END - Find Similar
     */

    /**
     * Identify Faces
    * To identify a face, a list of detected faces and a person group are used.
    * The list of similar faces are assigned to one person group person, 
    * to teach the AI how to identify future images of that person.
    * Uses the detectFaces() method from this quickstart.
    */
    public static void identifyFaces(FaceAPI client, String imageBaseURL) {
        // Create a dictionary to hold all your faces
        Map<String, String[]> facesList = new HashMap<String, String[]>();
        facesList.put("Family1-Dad", new String[] { "Family1-Dad1.jpg", "Family1-Dad2.jpg" });
        facesList.put("Family1-Mom", new String[] { "Family1-Mom1.jpg", "Family1-Mom2.jpg" });
        facesList.put("Family1-Son", new String[] { "Family1-Son1.jpg", "Family1-Son2.jpg" });
        facesList.put("Family1-Daughter", new String[] { "Family1-Daughter1.jpg", "Family1-Daughter2.jpg" });
        facesList.put("Family2-Lady", new String[] { "Family2-Lady1.jpg", "Family2-Lady2.jpg" });
        facesList.put("Family2-Man", new String[] { "Family2-Man1.jpg", "Family2-Man2.jpg" });

        // A group photo that includes some of the persons you seek to identify from your dictionary.
        String groupPhoto = "identification1.jpg";

        // Create a person group ID for a new person group (the group that holds all the person group persons).
        String personGroupID = "my-families";
        System.out.println("Creating the person group " + personGroupID + " ...");
        // Create the person group, so our photos have one to belong to.
        client.personGroups().create(personGroupID, new CreatePersonGroupsOptionalParameter().withName(personGroupID));
        
        // Group the faces. Each array of similar faces will be grouped into a single person group person.
        for (String personName : facesList.keySet()) {
            // Associate the family member name with an ID, by creating a Person object.
            UUID personID = UUID.randomUUID();
            Person person = client.personGroupPersons().create(personGroupID, 
                    new CreatePersonGroupPersonsOptionalParameter().withName(personName));

            for (String personImage : facesList.get(personName)) {
                // Add each image in array to a person group person (represented by the key and person ID).
                client.personGroupPersons().addPersonFaceFromUrl(personGroupID, person.personId(), imageBaseURL + personImage, null);
            } 
        }  

        // Once images are added to a person group person, train the person group.
        System.out.println();
        System.out.println("Training person group " + personGroupID + " ...");
        client.personGroups().train(personGroupID);

        // Wait until the training is completed.
        while(true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) { e.printStackTrace(); }
            
            // Get training status
            TrainingStatus status = client.personGroups().getTrainingStatus(personGroupID);
            if (status.status() == TrainingStatusType.SUCCEEDED) {
                System.out.println("Training status: " + status.status());
                break;
            }
            System.out.println("Training status: " + status.status());
        }
        System.out.println();

        // Detect faces from our group photo (which may contain one of our person group persons)
        List<UUID> detectedFaces = detectFaces(client, imageBaseURL + groupPhoto, groupPhoto);
        // Identifies which faces in group photo are in our person group. 
        List<IdentifyResult> identifyResults = client.faces().identify(personGroupID, detectedFaces, null);
        // Print each person group person (the person ID) identified from our results.
        System.out.println("Persons identified in group photo " + groupPhoto + ": ");
        for (IdentifyResult person : identifyResults) {
            System.out.println("Person ID: " + person.faceId().toString() 
                        + " with confidence " + person.candidates().get(0).confidence());
        }
    }
    /**
     * END - Identify Faces
     */
}
