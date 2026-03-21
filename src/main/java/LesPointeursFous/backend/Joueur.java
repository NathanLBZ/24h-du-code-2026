package LesPointeursFous.backend;

import LesPointeursFous.backend.item.*;

import java.util.List;
import java.util.ArrayList;

public class Joueur {
    private Vaisseau[] vaisseaux;
    private List<Planete> planetes;
    private List<Plan> plans;
    private int monnaie;

    public Joueur(Vaisseau v1, Vaisseau v2, Planete p){
        this.vaisseaux = new Vaisseau[10];
        this.vaisseaux[0] = v1;
        this.vaisseaux[1] = v2;
        this.planetes = new ArrayList<>();
        this.planetes.add(p);
        this.plans = new ArrayList<>();
        this.monnaie = 100;
    }
    
    public int getMonnaie() {
        return monnaie;
    }

    
}
