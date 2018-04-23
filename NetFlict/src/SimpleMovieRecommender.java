import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SimpleMovieRecommender implements BaseMovieRecommender{

    Map<Integer, Movie> movies;
    static Map<Integer, User> users;

    /**
     * Fetches Movies from a file
     * @param movieFilename String of relative path and Name of the file
     * @return Map of MovieID and Movie
     */
    @Override
    public Map<Integer, Movie> loadMovies(String movieFilename){
        Map<Integer, Movie> moviesMap = new HashMap<>();    /* The Map */

        //Reading file by using Internal Library
        String contentBuilder = "";//                       Path is find directory
            //                           Files from libs    Path from libs
        try{//                         |--------(1)-------||----(2)--|  String name
            //                                                       |------(3)-----|
            contentBuilder = new String(Files.readAllBytes(Paths.get(movieFilename)));
            // BufferedReader br = new BufferedReader( new FileReader( movieFilename ) ); // replace by using String and read all file by Files IO
        }catch(IOException e){
            e.printStackTrace();
        }
        String[] lines = contentBuilder.split("\n");        /* Separating Each lines into String */

        //                                                  Pattern for CSV; For example
        //                                        101529,"Brass Teapot, The (2012)",Comedy|Fantasy|Thriller
        //                                           (1) , |---(2)---|    (3)       ||----(4)----|
        Pattern eachLinePattern = Pattern.compile("(\\d+),[\\\"?]?(.*)\\s\\((\\d+)\\)[\\\"?]?,(.+)");

        //Fields Variable for Movie Datatype
        int movieID, year;
        String mTitle, mGenres;

        //Pattern for the forth group of eachLinePattern; For example
        //comedy|Fantasy|Thriller
        //  (1)    (2)      (3)
        Pattern categoriesPattern = Pattern.compile("([^|]+)");
        Matcher matcher;

        for(int i = 1; i < lines.length; i++){
            matcher = eachLinePattern.matcher(lines[i]);
            if(!matcher.find()){
                continue;
            }

            movieID = Integer.parseInt(matcher.group(1));       /* Assigns group 1 into movieID field variable */
            mTitle = matcher.group(2);       /* Assigns group 2 into movieTitle field variable */
            year = Integer.parseInt(matcher.group(3));      /* Assigns group 3 into year field variable */
            mGenres = matcher.group(4);     /* Assigns group 4 into movieGenres field variable */
            moviesMap.put(movieID, new Movie(movieID, mTitle, year));    /* Stores all fields into the Map (the Key is movieID, and the value is a movie object) */

            matcher = categoriesPattern.matcher(mGenres);       /* Sets the pattern for the Categories String */

            if(mGenres.equals("(no genres listed)")){   /* If there is no categories to be assigned */
                continue;
            }

            //Assigns each categories to each Movie Object by using its movieID
            //System.out.println(movieGenres);
            while(matcher.find()){
                //System.out.println("Genres = " + matcher.group());
                moviesMap.get(movieID).addTag(matcher.group());        //now moviesMap will get MovieID to addTag by using Matcher
            }
        }

        return moviesMap;
    }

    /**
     * Fetches Users from a file
     * @param userFilename String of relative path and Name of the file
     * @return Map of UserID and User
     */
    @Override
    public Map<Integer, User> loadUsers(String userFilename){
        Map<Integer, User> usersMap = new HashMap<>();
        //Reading file by using StringBuilder (Unused)
        StringBuilder contentBuilder = new StringBuilder();
        // This method to read file is different from what is implemented loadMovies()
        // Because of inconsistencies of the \r and \n in each line (Some line has only "\n", some has "\r\n")
        try(Stream<String> stream = Files.lines(Paths.get(userFilename), StandardCharsets.UTF_8)){
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }catch(IOException e){
            e.printStackTrace();
        }
        String[] line = contentBuilder.toString().split("\n");        /* Separating Each lines into String */
        //Pattern for the each line of Rating.csv; For example
        //                           668,   108940, 2.5, 1391840917
        //                           (1)      (2)    (3)  (4)
        String eachLinePattern = "([\\d]+),([\\d]+),(.*),(\\d+)";
        Pattern pattern = Pattern.compile(eachLinePattern);

        //Fields Variable for Rating Datatype
        int userID;
        int movieId;
        double rating;
        long timestamp;

        for(int i = 1; i < line.length; i++){
            Matcher m = pattern.matcher(line[i]);
            if(m.matches()){
                userID = Integer.parseInt(m.group(1));      /*  Assigns group 1 into userID field variable */
                movieId = Integer.parseInt(m.group(2));     /* Assigns group 2 into movieId field variable */

                boolean isAvailableInDataBase = movies.containsKey(movieId) && movieId == movies.get(movieId).getID();
                if(!isAvailableInDataBase){       /* If movie ID is not in the map skip it */
                    continue;
                }
                rating = Double.parseDouble(m.group(3));        /* Assigns group 3 into rating field variable */
                timestamp = Long.parseLong(m.group(4));      /* Assigns group 4 into timestamp field variable */

                if(rating > 0){
                    if(usersMap.get(userID) == null){
                        usersMap.put(userID, new User(userID));
                    }
                    usersMap.get(userID).addRating(movies.get(movieId), rating, timestamp);
                }
            }
        }
        return usersMap;
    }

    @Override
    public void loadData(String movieFilename, String userFilename){
        movies = loadMovies(movieFilename); // movies map will load data to store in Map
        users = loadUsers(userFilename); // users map will load data to store in Map
    }

    @Override
    public Map<Integer, Movie> getAllMovies() {
        if (movies != null) { // if movie not equal to null
            return movies;    // return to Map
        }
        return new TreeMap<Integer, Movie>(); // if movie null create TreeMap to sort
        // return movies  you can use this return if you don't need to sort
    }

    @Override
    public Map<Integer, User> getAllUsers(){
        if (users != null) { // if user not equal to null
            return users;    // return to Map
        }
        return new TreeMap<>(); // to sort by using Treemap
        // return users you can use this return if you don't need to sort
    }

    @Override
    public void trainModel(String modelFilename){
        // Instantiates StringBuilder, the StringBuilder acts as a container for the Whole String
        StringBuilder modelBuilder = new StringBuilder();


        // Sorts the elements inside the set of UserID
        ArrayList<Integer> userIDArrayList = new ArrayList<>(users.keySet());
        Collections.sort(userIDArrayList);


        // Appends the Total user number
        modelBuilder.append("@NUM_USERS " + users.size() + "\n");

        // Appends the User map
        modelBuilder.append("@USER_MAP {");
        int countUser = 0;
        for(Integer userID : userIDArrayList){
            modelBuilder.append(countUser++ + "=" + users.get(userID).uid);
            if(!userID.equals(userIDArrayList.get(userIDArrayList.size() - 1))){
                modelBuilder.append(", "); // add comma to split data
            }else{
                modelBuilder.append("}\n");// add bracket and new line
            }

        }


        // Sorts the elements inside the set of MovieID
        ArrayList<Integer> movieIDArrayList = new ArrayList<>(movies.keySet()); // movies Map will allow to use keySet in ArrayList by create new one
        Collections.sort(movieIDArrayList);

        // Appends the Total movie number
        modelBuilder.append("@NUM_MOVIES "+ movies.size() + "\n");

        // Appends the Movie map
        modelBuilder.append("@MOVIE_MAP {");
        int countMovie = 0;
        for(Integer movieID : movieIDArrayList){
            modelBuilder.append(countMovie++ + "=" + movies.get(movieID).mid);
            if(!movieID.equals(movieIDArrayList.get(movieIDArrayList.size() - 1))){     /* If the Movie ID is not the last element of the sorted MovieID ArrayList */
                modelBuilder.append(", ");
            }else{
                modelBuilder.append("}\n");
            }
        }

        // Appends the Rating Matrix
        // From Matrix Let's; Row: Each User
        //                    Col: Each Movie (+ There is Mean Rating of the User after the last movie rating)
        modelBuilder.append("@RATING_MATRIX\n");
        for(Integer userID : userIDArrayList){
            //System.out.print("UserID: " + userID);
            for(Integer movieID : movieIDArrayList){
                if(users.get(userID).ratings.keySet().contains(movieID)){       /* If the User does rate the Movie */
                    modelBuilder.append(users.get(userID).ratings.get(movieID).rating + " ");
                }else{      /* If the User does not rate the Movie */
                    modelBuilder.append("0.0 ");
                }
            }
            // Appends the Mean of Rating which a user has given
            modelBuilder.append(users.get(userID).getMeanRating() + "\n");
        }

        // Appends
        modelBuilder.append("@USERSIM_MATRIX\n");
        for(Integer userID_I : userIDArrayList){
            //System.out.println(userID_I + " -----------------------");
            for(Integer userID_J : userIDArrayList){
                //System.out.print(userID_J + " ");
                modelBuilder.append(similarity(userID_I, userID_J) + " ");
            }
            //System.out.println("\n");
            modelBuilder.append("\n");
        }

        try{
            FileUtils.writeStringToFile(new File(modelFilename), modelBuilder.toString()); // Write file output to specific folder
        }catch(IOException e){
            e.printStackTrace(); // if cannot try go catch and print error point
        }
    }

    private HashMap<Integer, HashMap<Integer, Double>> similarityMatrix = new HashMap<>();
    //      User ID, Map of   UserID, Similarity Value
    // The map to store value of similarity read from the file
    private HashMap<Integer, Double> meanRatingMatrix = new HashMap<>();
    //      User ID, Mean Rating Value
    @Override
    public void loadModel(String modelFilename){
        String modelContent = null;
        try{
            modelContent = FileUtils.readFileToString(new File(modelFilename)); // Use FileUtils to Read all String data
        }catch(IOException e){
            e.printStackTrace();
        }

        // Find the line with "@USERSIM_MATRIX", then get its the line number of the line below it (Get the first line of the matrix)
        int similarityMatrixFirstLine = 0;
        String[] modelContentLines = modelContent.split("\n");
        for(int i = 0; i < modelContent.length(); i++){
            if(modelContentLines[i].equals("@USERSIM_MATRIX")){
                similarityMatrixFirstLine = i + 1;
                break;
            }
        }

        // Sort the userID by Ascending Order
        ArrayList<Integer> userIDArrayList = new ArrayList<>(users.keySet());
        Collections.sort(userIDArrayList);


        String similarityLine;
        for(Integer userID : userIDArrayList){      /* Iterates through the array of User ID */
            int j = 0;
            similarityLine = modelContentLines[similarityMatrixFirstLine++];
            String[] similarityLine_Array = similarityLine.split(" ");

            similarityMatrix.put(userID, new HashMap<>());
            for(int userID_nested : userIDArrayList){
                similarityMatrix.get(userID).put(userID_nested, Double.valueOf(similarityLine_Array[j++]));
            }
            // System.out.println(userID + " " + similarityMatrix.get(userID).size());
        }
        // Find the line with "@USERSIM_MATRIX", then get its the line number of the line below it (Get the first line of the matrix)
        int ratingMatrixFirstLine = 0;
        for(int i = 0; i < modelContent.length(); i++){
            if(modelContentLines[i].equals("@RATING_MATRIX")){
                ratingMatrixFirstLine = i + 1;
                break;
            }
        }

        // Sort the movieID by Ascending Order
        ArrayList<Integer> movieIDArrayList = new ArrayList<>(movies.keySet());
        Collections.sort(movieIDArrayList);

       String ratingLine;
        for(Integer userID : userIDArrayList){      /* Iterates through the array of Movie ID */
            ratingLine = modelContentLines[ratingMatrixFirstLine++];
            String[] ratingLine_Array = ratingLine.split(" ");

            ArrayList<Double> ratingArrayList = new ArrayList<>();
            for(int k = 0; k <= movies.size(); k++){
                ratingArrayList.add(Double.valueOf(ratingLine_Array[k]));
                //System.out.print(ratingLine_Array[k] + " ");
            }
            //System.out.println();
            meanRatingMatrix.put(userID, ratingArrayList.get(ratingArrayList.size() - 1));
        }
    }

    @Override
    public double predict(Movie movie, User user){
        //TODO: !!! FIX THIS !!! - In small testcase, there is an error when compared to given result!!
        double result;
        double remainder = 0;
        double denominator = 0;
        double similarity;

        if(meanRatingMatrix.get(user.uid) != null){
            for(Integer userID : users.keySet()){
                if(userID != user.uid && users.get(userID).ratings.keySet().contains(movie.mid)){
                    //similarity = similarity(u, user);
                    User currentUser = users.get(userID);
                    similarity = similarityMatrix.get(user.uid).get(currentUser.uid);
                    //System.out.println("Similarity between (" + u.uid + ", " + user.uid + "): " + similarity);
                    remainder += similarity * (currentUser.ratings.get(movie.mid).rating - meanRatingMatrix.get(currentUser.uid));
                    denominator += Math.abs(similarity);
                }
            }

            if(denominator == 0){
                result = meanRatingMatrix.get(user.uid);
            }else{
                result = meanRatingMatrix.get(user.uid) + (remainder / denominator);
            }

            if(result > 5){
                result = 5.0;
            }else if(result < 0.5){
                result = 0.5;
            }
            return result;
        }else{
            return user.getMeanRating();
        }
    }

    @Override
    public List<MovieItem> recommend(User u, int fromYear, int toYear, int K){ // in page 3 of PDF file it's about reccomend
        List<MovieItem> movieItemList = new ArrayList<>();
        for(Integer movieID : movies.keySet()){ // step 2 For each movie 𝑖 ∈ 𝐼∗, compute 𝑝 u,i.
            if(movies.get(movieID).year >= fromYear && movies.get(movieID).year <= toYear){
                //System.out.println(i++ + " " + predict(movies.get(movieID), u));
                movieItemList.add(new MovieItem(movies.get(movieID), predict(movies.get(movieID), u))); //step 1 Collect all the movie released during fromYear to toYear à 𝐼∗
            }
        }
        Collections.sort(movieItemList); // step 3 Rank the movies
        if(movieItemList.size() > K){ // if in step 4
            movieItemList.subList(K, movieItemList.size() - 1).clear(); // step 4 If|𝐼∗| > K, return the top K movies.Otherwise,return all the movies.
        }if(movieItemList.size() < K)
        {
            return movieItemList;
        }
        return movieItemList.subList( 0,K );    }

    private double similarity(int userID1, int userID2){

        //TODO: FIXED
        double similarity;


        User u1 = users.get(userID1);
        User u2 = users.get(userID2);
        if(userID1 == userID2){
            return similarity = 1;
        }


        TreeSet<Integer> commonRatedMovies = new TreeSet<>();
        commonRatedMovies.addAll(Sets.intersection(u1.ratings.keySet(), u2.ratings.keySet()));

        //Find the remainder
        double remainder = 0;
        double denominatorUser1 = 0;
        double denominatorUser2 = 0;
        for(int movieID : commonRatedMovies){
            remainder += ((u1.ratings.get(movieID).rating - u1.getMeanRating()) * (u2.ratings.get(movieID).rating - u2.getMeanRating()));
            denominatorUser1 += (u1.ratings.get(movieID).rating - u1.getMeanRating()) * (u1.ratings.get(movieID).rating - u1.getMeanRating());
            denominatorUser2 += (u2.ratings.get(movieID).rating - u2.getMeanRating()) * (u2.ratings.get(movieID).rating - u2.getMeanRating());
        }
        double denominator = (Math.sqrt(denominatorUser1 * denominatorUser2));
        if(denominator != 0){
            similarity = remainder / denominator;
        }else{
            similarity = 0.0;
        }
        return similarity;
    }

    public ArrayList<Double> ratingToArrayList(Collection<Rating> ratingSet){
        ArrayList<Double> doubleArrayList = new ArrayList<>();
        for(Rating rating : ratingSet){
            doubleArrayList.add(rating.rating);
        }
        return doubleArrayList;
    }

    public HashMap<Integer, HashMap<Integer, Double>> getSimilarityMatrix(){
        return similarityMatrix;
    }

    public HashMap<Integer, Double> getMeanRatingMatrix(){
        return meanRatingMatrix;
    }
}
