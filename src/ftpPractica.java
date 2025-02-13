
import org.apache.commons.net.ftp.*;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ftpPractica {

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

    public static String getFTPServerFromSTDIn(Scanner sc) {
        System.out.println("¿A qué IP o Host quieres conectarte?");
        return sc.nextLine();
    }

    public static String loopUntilValidURL(Scanner sc) {
        String output;
        while (!handleValidIP(output = getFTPServerFromSTDIn(sc))) {
            System.out.println("No se ha encontrado una dirección válida, inténtalo otra vez");
        }
        ;
        return output;
    }

    public static int actionMenu(Scanner sc) {
        System.out.println("Escribe el número de la acción a realizar");
        System.out.println("1. Listar archivos");
        System.out.println("2. Descargar archivo");
        System.out.println("3. Subir archivo");
        System.out.println("0. Salir");
        return sc.nextInt();
    }

    public static void doAction(Scanner sc, FTPClient client) throws IOException {
        switch (actionMenu(sc)) {
            case 1: listFilesInFTPClient(client);
        }
    }

    public static boolean connectToFTPServer(Scanner sc, FTPClient client) {
        //Loop
        String FTPServer = loopUntilValidURL(sc);
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
        Scanner sc = new Scanner(System.in);

        while (!connectToFTPServer(sc, client)) ;
        client.enterLocalPassiveMode();

        try {
            while ((attemptLogin(client, sc)) - 499 > 0) {
                System.out.println("Error conectándose al servidor, vuelve a intentarlo");
            }
        } catch (IOException e) {
            System.err.println("\nLa conexión se ha cerrado de forma inesperada (¿Has fallado el login muchas veces?)");
            client.disconnect();
            return;
        }

        doAction(sc, client);

        client.disconnect();
        System.out.println("fin de la conexión");
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
    public static int attemptLogin(FTPClient client, Scanner sc) throws IOException {
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