package LesPointeursFous.backend;

import java.util.List;

public class Planete {
    private int nbPV;
    private int nbEmplacements;
    private int nbEmplacementsLibres;
    private int ressources;
    private List<Module> modules;

    public Planete (int PV, int nbEmplacements, int nbEmplacementsLibres, int ressources, List<Module> modules) {
        this.nbPV = PV;
        this.nbEmplacements = nbEmplacements;
        this.nbEmplacementsLibres = nbEmplacementsLibres;
        this.ressources = ressources;
        this.modules = modules;
    }

    // Getters et Setters
    public int getNbPV() {
        return nbPV;
    }

    public int getNbEmplacements() {
        return nbEmplacements;
    }

    public int getNbEmplacementsLibres() {
        return nbEmplacementsLibres;
    }

    public int getRessources() {
        return ressources;
    }

    public List<Module> getModules() {
        return modules;
    }
}
