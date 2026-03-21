package LesPointeursFous;

import LesPointeursFous.services.ApiClient;
import LesPointeursFous.services.ApiVaisseau;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Hello World!" );

        Dotenv dotenv = Dotenv.configure().load();
        ApiClient apiFetch = new ApiClient(dotenv.get("API_URL"), dotenv.get("API_KEY"));

        ApiVaisseau vaisseau = new ApiVaisseau(apiFetch);
        
        vaisseau.deplacer("c1b647f1-1748-492a-b5a9-2a9af9b5e5ed", "6ea98de8-de02-4061-9991-21f27a3ecf65", 36, 3);
        
        System.out.println(vaisseau.getVaisseaux("c1b647f1-1748-492a-b5a9-2a9af9b5e5ed"));
    }
}
