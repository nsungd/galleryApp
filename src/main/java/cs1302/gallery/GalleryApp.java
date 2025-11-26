package cs1302.gallery;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.lang.Math;
import java.lang.Runnable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.TilePane;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Animation;
import javafx.util.Duration; 
import com.google.gson.Gson;
import com.google.gson.GsonBuilder; 
import java.io.IOException;
import java.lang.InterruptedException; 

/**                                                                                                                                                                                                         
 * Represents an iTunes Gallery App.                                                                                                                                                                        
 */
 public class GalleryApp extends Application {
  
      /** HTTP client. */
      public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible                                                                                                        
          .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP                                                                                                        
          .build();                                     // builds and returns a HttpClient object                                                                                                             
  
      /** Google {@code Gson} object for parsing JSON-formatted strings. */
      public static Gson GSON = new GsonBuilder()
          .setPrettyPrinting()                          // enable nice output when printing                                                                                                                   
          .create();                                    // builds and returns a Gson object                                                                                                                   
  
      /**                                                                                                                                                                                                     
       * Represents a response from the iTunes Search API.                                                                                                                                                    
       */
      private static class ItunesResponse {
    	  int resultCount;
          ItunesResult[] results;
      }
          	/**                                                                                                                                                                                                     
          	 * Represents a result in response, allows Gson to create an object from                                                                                                                                
          	 * JSON body.                                                                                                                                                                                           
          	 */
      private static class ItunesResult {
    	  String artworkUrl100;
      }
      
      //Instance variables                                                                                                                                                                                    
      private Stage stage;
      private Scene scene;
      private VBox root;
      private HBox topHbox;
      private HBox textHbox;
      private HBox botHbox;
      private ProgressBar loadingBar;
      private ComboBox<String> dropdown;
      private TextField searchBar;
      private Label searchLabel;
      private Label status;
      private Label itunesLabel;
      private Button playButton;
      private Button getImagesButton;
      private Image[] images;
      private ImageView[] imgViews;
      private TilePane gallery;
      private Alert alert;
      private Timeline timeline;
      
      /**                                                                                                                                                                                                    
       * Constructs a {@code GalleryApp} object}.                                                                                                                                                            
       */
      public GalleryApp() {
    	  this.stage = null;
    	  this.scene = null;
    	  this.root = new VBox();
    	  this.topHbox = new HBox();
    	  this.textHbox = new HBox();
    	  this.botHbox = new HBox();
    	  this.loadingBar = new ProgressBar(0);
    	  this.dropdown = new ComboBox<String>();
    	  this.searchBar = new TextField();
    	  this.searchLabel = new Label("Search:");
    	  searchLabel.setLabelFor(searchBar);
    	  this.status = new Label("Type in a term, select a media type, then click the button");
    	  this.itunesLabel = new Label("Images provided by iTunes Search API.");
    	  this.playButton = new Button("Play");
    	  this.getImagesButton = new Button("Get Images");
    	  this.images = new Image[0];
    	  this.imgViews = new ImageView[20];
    	  this.gallery = new TilePane();
    	  this.alert = new Alert(AlertType.ERROR);
    	  this.timeline = new Timeline();
      } // GalleryApp                                                                                                                                                                                        
      
      /** {@inheritDoc} */
      @Override
      public void init() {
    	  // Adds all children and edits properties                                                                                                                                                          
    	  System.out.println("init() called");
    	  root.getChildren().addAll(topHbox, textHbox, gallery, botHbox);
    	  topHbox.getChildren().addAll(playButton, searchLabel, searchBar, dropdown, getImagesButton);
    	  searchBar.setPrefWidth(187);
    	  playButton.setDisable(true);
    	  dropdown.getItems().addAll("movie", "podcast", "music", "musicVideo", "audioBook",
    			  "shortFilm", "tvShow", "software", "ebook", "all");
    	  dropdown.setPromptText("music");
    	  dropdown.setValue("music");
    	  textHbox.getChildren().addAll(status);
    	  gallery.setPrefColumns(5);
    	  gallery.setPrefRows(4);
    	  gallery.setPrefTileWidth(100);
    	  gallery.setPrefTileHeight(100);
    	  loadingBar.setPrefWidth(240);
    	  
    	  // Adds imgViews to gallery                                                                                                                                                                        
    	  for (int i = 0; i < imgViews.length; i++) {
    		  imgViews[i] = new ImageView("file:resources/default.png");
    		  gallery.getChildren().addAll(imgViews[i]);
    	  }
    	  botHbox.getChildren().addAll(loadingBar, itunesLabel);
    	  
    	  Runnable task = () -> {
    		  this.download();
    	  };
    	  
    	  EventHandler<ActionEvent> player = (ActionEvent ae) -> {
    		  this.playReplace();
    	  };
    	  
          KeyFrame keyFrame = new KeyFrame(Duration.seconds(2), player);
          timeline.setCycleCount(Timeline.INDEFINITE);
          timeline.getKeyFrames().add(keyFrame);
          
          getImagesButton.setOnAction(event -> runNow(task));
          
          // Changes between pause and play functions                                                                                                                                                        
          playButton.setOnAction(event -> {
        	  if (timeline.getStatus() == Timeline.Status.RUNNING) {
        		  timeline.pause();
        		  playButton.setText("Play");
        	  } else {
        		  timeline.play();
        		  playButton.setText("Pause");
        	  }
          });
          // Displays error message and deactivates app.                                                                                                                                                     
          if (alert.isShowing()) {
        	  playButton.disarm();
        	  getImagesButton.disarm();
        	  searchBar.setDisable(true);
          } else {
        	  playButton.arm();
        	  getImagesButton.arm();
        	  searchBar.setDisable(false);
          }
      } // init                                                                                                                                                                                              
      
      /** {@inheritDoc} */
      @Override
      public void start(Stage stage) {
    	  this.stage = stage;
    	  this.scene = new Scene(this.root);
    	  this.stage.setOnCloseRequest(event -> Platform.exit());
    	  this.stage.setTitle("GalleryApp!");
    	  this.stage.setScene(this.scene);
    	  this.stage.sizeToScene();
    	  this.stage.show();
    	  Platform.runLater(() -> this.stage.setResizable(false));
      } // start                                                                                                                                                                                             
      
      /**                                                                                                                                                                                                    
       * Downloads images from iTunes and displays them from imgViews.                                                                                                                                       
       */
      public void download() {
    	  String term = URLEncoder.encode(searchBar.getText(), StandardCharsets.UTF_8);
    	  String limit = URLEncoder.encode("200", StandardCharsets.UTF_8);
    	  String media = URLEncoder.encode(dropdown.getValue().toString(), StandardCharsets.UTF_8);
    	  String query = String.format("?term=%s&limit=%s&media=%s", term, limit, media);
    	  
    	  URI location = URI.create("https://itunes.apple.com/search" + query);
    	  if (timeline.getStatus() == Timeline.Status.RUNNING) {
    		  timeline.pause();
    		  Platform.runLater(() -> playButton.setText("Play"));
    	  }
    	  Platform.runLater(() -> status.setText("Getting images..."));
    	  try {
    		  HttpRequest request = HttpRequest.newBuilder().uri(location).build();
    		  HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
    		  ensureGoodResponse(response);
              String body = response.body();
              ItunesResponse itunesResponse = GSON.fromJson(body, GalleryApp.ItunesResponse.class);
              if (itunesResponse.resultCount > 20) {
            	  if (itunesResponse.resultCount > 200) {
            	   		images = new Image[200];
            	 	} else {
            	 		images = new Image[itunesResponse.resultCount];
            	 	}
            	  Platform.runLater(() -> loadingBar.setProgress(0));
            	  for (int i = 0; i < itunesResponse.results.length && i < 200; i++) {
            		  ItunesResult result = itunesResponse.results[i];
            		  images[i] = new Image(result.artworkUrl100);
            		  Platform.runLater(() ->
            		  loadingBar.setProgress(loadingBar.getProgress() + (1.0 / images.length)));
            	  }
            	  for (int i = 0; i < imgViews.length; i++) {
            		  imgViews[i].setImage(images[i]);
            	  }
            	  playButton.setDisable(false);
            	  Platform.runLater(() -> status.setText(location.toString()));
            	  Platform.runLater(() -> playButton.setText("Play"));
              } else {
            	  Platform.runLater(() -> status.setText("Last attempt to get imgaes failed..."));
            	  alert.setContentText("URI: " + location.toString() +
            			  "\n\nException: java.lang.IllegalArgumentException: " +
            			  itunesResponse.results.length + " distinct results found, but 21 " +
            			  "or more are needed.");
            	  Platform.runLater(() -> alert.show());
              }
    	  } catch (InterruptedException | IOException exc) {
    		  System.out.println("interuppted");
    		  status.setText("Last attempt to get images failed...");
    	  }
      }
             
      /**                                                                                                                                                                                                    
       * Swaps an image in the gallery with another downloaded image                                                                                                                                         
       * that is not displayed.                                                                                                                                                                              
       */
      public void playReplace() {
    	  playButton.setText("Pause");
          int viewIndex = (int) (Math.random() * 20);
          int randomIndex = ((int) (Math.random() * (images.length - 20))) + 20;
          Image tempImg = imgViews[viewIndex].getImage();
           
          imgViews[viewIndex].setImage(images[randomIndex]);
          images[randomIndex] = tempImg;
      }
      
      /**                                                                                                                                                                                                    
       * Creates and starts new daemon thread.                                                                                                                                                               
       *                                                                                                                                                                                                     
       * @param target object whose {@code run} method is invoked                                                                                                                                            
       */
      public static void runNow(Runnable target) {
    	  Thread thread = new Thread(target);
    	  thread.setDaemon(true);
    	  thread.start();
      }
      
      /**                                                                                                                                                                                                    
       * Throw an {@link java.io.IOException} if the HTTP status code of the                                                                                                                                 
       * {@link java.net.http.HttpResponse} supplied by {@code response} is                                                                                                                                  
       * not {@code 200 OK}.                                                                                                                                                                                 
       * @param <T> response body type                                                                                                                                                                       
       * @param response response to check                                                                                                                                                                   
       * @see <a href="https://httpwg.org/specs/rfc7231.html#status.200">[RFC7232] 200 OK</a>                                                                                                                
       */
      private static <T> void ensureGoodResponse(HttpResponse<T> response) throws IOException {
    	  if (response.statusCode() != 200) {
    		  throw new IOException(response.toString());
    	  } // if                                                                                                                                                                                            
      } // ensureGoodResponse     
      
      /** {@inheritDoc} */
      @Override
      public void stop() {
    	  // feel free to modify this method                                                                                                                                                                 
    	  System.out.println("stop() called");
      } // stop                                                                                                                                                                                              
      
 } // GalleryApp   {
