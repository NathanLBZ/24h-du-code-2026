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
        
        String idEquipe = "c1b647f1-1748-492a-b5a9-2a9af9b5e5ed";

        vaisseau.deplacer(idEquipe, "128942bd-0f0c-43a8-b1d2-d6f2f5fec732", 3, 3);


        System.out.println(vaisseau.getVaisseaux(idEquipe));
    }
}
