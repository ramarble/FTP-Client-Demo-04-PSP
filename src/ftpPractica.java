
import org.apache.commons.net.ftp.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ftpPractica {


    static Scanner sc = new Scanner(System.in);
    public static boolean anonymousMode = false;

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

    public static String loopUntilValidURL() {
        String output;
        while (!handleValidIP(output = getFTPServerFromSTDIn())) {
            System.out.println("No se ha encontrado una dirección válida, inténtalo otra vez");
        }
        return output;
    }

    private static String getNewFilePath() throws IOException {
        File file1 = new File("temp");
        File file2 = new File(file1.getCanonicalPath());
        String path = file2.getParentFile().getCanonicalPath();
        file1.delete();
        System.out.println("¿Dónde quieres guardar el archivo? (Por defecto: " + path + "/): ");
        System.out.println("Escrito como /ruta/al/directorio/");
        sc.nextLine();
        String newPath = sc.nextLine();
        if ((new File(newPath).isDirectory())) {
            return newPath;
        } else return path;
    }

    private static int downloadFile(FTPClient client) throws IOException {
        String pathOutput = getNewFilePath();
        System.out.println("Di el nombre del archivo que quieres extraer del remoto");
        String fileName = sc.nextLine();
        FileOutputStream fos = new FileOutputStream(pathOutput + fileName);
        client.retrieveFile(fileName, fos);
        return client.getReplyCode();
    }

    private static void uploadFile() {

    }

    private static void loopActionMenu(FTPClient client) throws IOException {

        int indexSelected = 99;
        while (indexSelected!=0) {
            System.out.println("Escribe el número de la acción a realizar");
            System.out.println("1. Listar archivos");
            System.out.println("2. Descargar archivo");
            if (!anonymousMode) {
                System.out.println("3. Subir archivo");
            }
            System.out.println("0. Salir");
            doAction(indexSelected = sc.nextInt(), client);
        }
    }

    private static boolean actionSucceededForFTP(int code) {
        return !(code - 499 > 0);
    }

    private static void doAction(int indexSelected, FTPClient client) throws IOException {
        switch (indexSelected) {
            case 1: listFilesInFTPClient(client); break;
            case 2: downloadFile(client); break;
            case 3: if (!anonymousMode) {
                uploadFile(); break;
            }
            case 0:
                System.out.println("Saliendo del programa"); break;
            default:
                System.out.println("Índice incorrecto, intentalo de nuevo");
        }
    }

    public static boolean connectToFTPServer(FTPClient client) {
        //Loop
        String FTPServer = loopUntilValidURL();
        //Overridden for testing purposes
        FTPServer = "192.168.146.143";
        try {
            client.connect(FTPServer);
        } catch (IOException e) {
            System.err.println("No se ha podido encontrar el servidor");
            return false;
        }
        System.out.println("Dirección válida, vamos a intentar conectarnos al servidor");
        return true;
    }

    public static void main(String[] args) throws IOException {
        FTPClient client = new FTPClient();

        while (!connectToFTPServer(client)) ;
        client.enterLocalPassiveMode();

        try {
            while ((attemptLogin(client)) - 499 > 0) {
                System.out.println("Error conectándose al servidor, vuelve a intentarlo");
            }
        } catch (IOException e) {
            System.err.println("\nLa conexión se ha cerrado de forma inesperada (¿Has fallado el login muchas veces?)");
            client.disconnect();
            return;
        }

        loopActionMenu(client);

        //loop finished
        sc.close();
        client.disconnect();
    }



    public static void listFilesInFTPClient(FTPClient client) throws IOException {
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
    public static int attemptLogin(FTPClient client) throws IOException {
        System.out.print("Username: (dejar en blanco para anónimo) ");
        String password;
        String username = sc.nextLine();
        if (username.isBlank()) {
            username = "ftp";
            password = "raul.marrupe@ftpclient";
            anonymousMode = true;

            System.out.println("Intentando login en modo anónimo...");
        } else {
            System.out.print("Password: ");
            password = sc.nextLine();
            anonymousMode = false;
            System.out.println("Intentando login en modo usuario para " + username + " ...");
        }
        client.login(username, password);
        return client.getReplyCode();
    }
}