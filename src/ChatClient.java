import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ChatClient extends Application {
  /** Pixels between buttons. */
  private static final int SPACE_BETWEEN_BUTTONS = 10;
  // IO streams
  private DataOutputStream toServer = null;
  private DataInputStream fromServer = null;
  // Text area to display contents
  private TextArea ta = new TextArea();
  Socket socket;

  @Override // Override the start method in the Application class
  public void start(Stage primaryStage) {
    Parameters params = getParameters();
    List<String> list = params.getRaw();
    String userName = "";
    int portNumber;
    if (list.size() >= 1) {
      userName = list.get(0);
    }
    String disconnectStr = "disconnect " + userName + "\n";
    String clientName = "Chat Client: " + userName;
    if (list.size() >= 2) {
      portNumber = Integer.parseInt(list.get(1));
    } else {
      portNumber = 4688;
    }
    // Panel p to hold the label, text field and buttons
    HBox paneForTextField = new HBox(SPACE_BETWEEN_BUTTONS);
    paneForTextField.setPadding(new Insets(5, 5, 5, 5));
    Label label = new Label("Enter: ");
    Button disconnectButton = new Button("Disconnect");
    TextField tf = new TextField();
    tf.setPrefColumnCount(22);
    paneForTextField.getChildren().
      addAll(label, tf, disconnectButton);
    paneForTextField.setAlignment(Pos.CENTER);

    BorderPane mainPane = new BorderPane();
    mainPane.setCenter(new ScrollPane(ta));
    mainPane.setTop(paneForTextField);

    // Create a scene and place it in the stage
    Scene scene = new Scene(mainPane, 450, 200);
    primaryStage.setTitle(clientName); // Set the stage title
    primaryStage.setScene(scene); // Place the scene in the stage
    primaryStage.show(); // Display the stage

    disconnectButton.setOnAction(e -> {
      try {
        if (socket.isConnected()) {
          toServer.writeBytes(disconnectStr);
          toServer.flush();
        }
      } catch (IOException ex) {
        System.err.println(ex);
      }
//      Stage stage = (Stage) disconnectButton.getScene().getWindow();
//      stage.close();
    });
    tf.setOnAction(e -> {
      try {
        // Get the message from the text field
        String message = tf.getText() + "\n";
        synchronized (this) {
          ta.appendText(message);
        }
        tf.clear();
        // Send the message to the server
        toServer.writeBytes(message);
        toServer.flush();
      } catch (IOException ex) {
        System.err.println(ex);
      }
    });

    // Create a socket to connect to the server
    try {

      socket = new Socket("localhost", portNumber);
      // Create an input stream to receive data from the server
      fromServer = new DataInputStream(socket.getInputStream());
      // Create an output stream to send data to the server
      toServer = new DataOutputStream(socket.getOutputStream());

      toServer.writeBytes("connect " + userName + "\n");
      toServer.flush();
    } catch (IOException ex) {
      ta.appendText(ex.toString() + '\n');
    }

    Thread thread = new Thread(new MessageReceiver());
    thread.start();
  }

  private class MessageReceiver implements Runnable {
    public void run() {
      try {
        while (socket.isConnected()) {
          int length = fromServer.available();
          if (length > 0) {
            byte[] receiveBuf = new byte[length];
            fromServer.readFully(receiveBuf);
            synchronized (this) {
              for (byte b : receiveBuf) {
                ta.appendText(Character.toString((char)b));
              }
              ta.appendText("\n");
            }
          }
        }
      } catch (IOException e) {
        synchronized (this) {
          ta.appendText(e.toString() + '\n');
        }
      }
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Please include a Client name in the command line.");
      System.exit(0);
    } else {
      launch(args);
    }
  }

}
