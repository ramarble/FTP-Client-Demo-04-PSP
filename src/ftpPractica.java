
import org.apache.commons.net.ftp.*;

import java.io.*;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ftpPractica {

    static Scanner sc = new Scanner(System.in);
    public static boolean anonymousMode = false;

    //Cute method that returns if a code is not in the 500s, which is the error range.
    public static boolean actionSucceededForFTP(int code) {
        return !(code - 499 > 0);
    }

    public static void main(String[] args) throws IOException {
        FTPClient client = new FTPClient();

        //Loops until it connects to a server or fails
        while (!connectToFTPServer(client));

        //Sets passive mode and allows for upload of TXT and Binary files
        changeClientSettings(client);

        //Handles the login, exceptions, errors, username and password, anonymous mode
        handleLoginToServer(client);

        //Once logged in, loop actions available
        loopActionMenu(client);

        //loop finished
        sc.close();
        client.disconnect();
    }

    public static boolean connectToFTPServer(FTPClient client) {
        //Loop
        String FTPServer = loopUntilValidURL();
        try {
            client.connect(FTPServer);
        } catch (IOException e) {
            System.err.println("No se ha podido encontrar el servidor");
            return false;
        }
        System.out.println("Dirección válida, vamos a intentar conectarnos al servidor");
        return true;
    }

    //Gets user input until it receives a valid URL
    public static String loopUntilValidURL() {
        String output;
        while (!handleValidIP(output = getFTPServerFromSTDIn())) {
            System.out.println("No se ha encontrado una dirección válida, inténtalo otra vez");
        }
        return output;
    }

    //Returns whether it found a match for a valid IP or URL. It works... about 10% of the time
    public static boolean handleValidIP(String input) {
        //Regex that matches if it's a valid IP
        Pattern ipPattern = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$", Pattern.CASE_INSENSITIVE);
        //Regex that matches if it's a valid URL. This will always trigger for ipPattern so it has to go second in the if statement
        Pattern urlPattern = Pattern.compile("(\\b(https?|ftp|file)://)?[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]", Pattern.CASE_INSENSITIVE);
        boolean foundIP = ipPattern.matcher(input).find();
        boolean foundURL = urlPattern.matcher(input).find();

        return foundIP || foundURL;
    }

    public static String getFTPServerFromSTDIn() {
        System.out.println("¿A qué IP o Host quieres conectarte?");
        return sc.nextLine();
    }

    // connectToFTPServer succeeds here

    private static void changeClientSettings(FTPClient client) throws IOException {
        client.enterLocalPassiveMode();
        client.setFileType(FTP.ASCII_FILE_TYPE, FTP.BINARY_FILE_TYPE);
        client.setFileTransferMode(FTP.BINARY_FILE_TYPE);
    }

    //Output that handles the correct login to a server that we've already connected to
    private static void handleLoginToServer(FTPClient client) throws IOException {
        try {
            while ((!actionSucceededForFTP(attemptLogin(client)))) {
                System.out.println("Error conectándose al servidor, vuelve a intentarlo");
            }
        } catch (IOException e) {
            System.err.println("\nLa conexión se ha cerrado de forma inesperada (¿Has fallado el login muchas veces?)");
            client.disconnect();
        }
    }

    //Gets user input for a valid username and password, has automatic handling for anonymous mode.
    private static int attemptLogin(FTPClient client) throws IOException {
        System.out.print("Username: (dejar en blanco para anónimo) ");
        String password;
        String username = sc.nextLine();
        //Automatic handling for anonymous mode
        if (username.isBlank()) {
            username = "ftp";
            password = "raul.marrupe@ftpclient";
            anonymousMode = true;
        } else {
            System.out.print("Password: ");
            password = sc.nextLine();
            //Even when logging in with a username and password, it can still be an anonymous connection.
            anonymousMode = username.equals("ftp") || username.equals("anonymous");
        }
        if (anonymousMode) {
            System.out.println("Intentando login en modo anónimo...");
        } else {
            System.out.println("Intentando login en modo usuario para " + username + " ...");
        }
        client.login(username, password);
        return client.getReplyCode();
    }

    // handleLoginToServer succeeds here

    //Shows an action menu with the available options to interact with the server
    private static void loopActionMenu(FTPClient client) throws IOException {

        int indexSelected = 99;
        try {
            while (indexSelected != 0) {
                System.out.println("Escribe el número de la acción a realizar");
                System.out.println("1. Listar archivos");
                System.out.println("2. Descargar archivo");
                if (!anonymousMode) {
                    System.out.println("3. Subir archivo");
                }
                System.out.println("0. Salir");
                selectMenuItem(indexSelected = sc.nextInt(), client);
            }
        } catch (InputMismatchException e) {
            System.out.println("Input erróneo, intentalo de nuevo");
            sc.nextLine();
            loopActionMenu(client);
        }
    }

    //Handles user input for the actions available to interact with the server
    //The cases are each their own method for readability.
    private static void selectMenuItem(int indexSelected, FTPClient client) throws IOException {
        switch (indexSelected) {
            case 1: listFilesInFTPClient(client); break;
            case 2: handleFileDownload(client);break;
            case 3: if (!anonymousMode) {handleFileUpload(client); break;}
            case 0: System.out.println("Saliendo del programa"); break;
            default: System.out.println("Índice incorrecto, intentalo de nuevo");
        }
    }

    private static void listFilesInFTPClient(FTPClient client) throws IOException {
        FTPFile[] reply = client.listFiles();
        for (FTPFile s : reply) {
            System.out.println(s.getName());
        }

        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            client.disconnect();
            System.out.println("Conexión rechazada");
            System.exit(0);
        }
    }

    private static void handleFileDownload(FTPClient client) throws IOException {
        int reply = attemptFileDownload(client);
        if (actionSucceededForFTP(reply)) {
            System.out.println("Archivo descargado correctamente");
        } else {
            System.err.println("Error descargando el archivo (¿Archivo no encontrado?)");
        }
    }

    private static int attemptFileDownload(FTPClient client) throws IOException {
        String pathOutput = getValidPathToStoreFile(true);
        System.out.println("Di el nombre del archivo que quieres extraer del remoto");
        String fileName = sc.nextLine();
        FileOutputStream fos = new FileOutputStream(pathOutput + fileName);
        client.retrieveFile(fileName, fos);
        return client.getReplyCode();
    }

    //Handles (and loops) user input for a valid path to store a file from the FTP server
    private static String getValidPathToStoreFile(boolean firstPass) throws IOException {
        //This seems to be the best way I found to get the current working directory.
        File file1 = new File("temp");
        File file2 = new File(file1.getCanonicalPath());
        String path = file2.getParentFile().getCanonicalPath();
        file1.delete();
        System.out.println("¿Dónde quieres guardar el archivo? (Por defecto: " + path + "/): \nEscrito como /ruta/al/directorio/");
        //This line is needed because scanner is a horrible way to do anything in java, and the buffer is horrible to deal with.
        if(firstPass) {
            sc.nextLine();
        }
        String newPath = sc.nextLine();
        if ((new File(newPath).isDirectory())) {
            System.out.println("Ruta seleccionada es: " + newPath);
            return newPath;
        } else if (newPath.isBlank()) {
            System.out.println("Ruta seleccionada es: " + path);
            return path;
        } else {
            System.err.println("Ruta no encontrada, intentalo de nuevo");
            return getValidPathToStoreFile(false);
        }
    }

    private static void handleFileUpload(FTPClient client) throws IOException {
        if (attemptFileUpload(client)) {
            System.out.println("Archivo subido correctamente");
        } else {
            System.err.println("Error subiendo el archivo, no se ha encontrado o no se ha podido subir al servidor.");
        }
    }

    //Handles finding a valid file to upload to the server, exits without an action if it doesn't.
    private static boolean attemptFileUpload(FTPClient client) throws IOException {
        System.out.println("Di el nombre del archivo que quieres subir (/ruta/al/archivo/archivo.extension)");
        sc.nextLine();
        File fileUp = new File(sc.nextLine());
        if ((fileUp.exists())) {
            System.out.println("Archivo encontrado");
            if (client.storeFile(fileUp.getName(), new FileInputStream(fileUp))) {
                return true;
            } else {
                System.err.println("Error subiendo el archivo al servidor");
            }
        }
        return false;
    }
}