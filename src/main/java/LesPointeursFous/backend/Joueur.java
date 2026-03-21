package LesPointeursFous.backend;

import LesPointeursFous.backend.item.*;

import java.util.List;
import java.util.ArrayList;

public class Joueur {
    private Vaisseau[10] vaisseaus;
    private List<Planete> planetes;
    private List<Plan> plans;
    private int monnaie;

    public Joueur(Vaisseau v1, Vaisseau v2, Planete p){
        this.vaisseaus[0] = v1;
        this.vaisseaus[1] = v2;
        this.planetes = new ArrayList<>().add(p);
        this.plans = new ArrayList<>();
        this.monnaie = 100;
    }
    
}
