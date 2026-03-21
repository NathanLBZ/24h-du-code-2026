package LesPointeursFous.backend;

public class Vaisseau {
    private static Carte carte;
    private int nbPV;
    private int x;
    private int y;

    public Vaisseau(int x, int y, int nbPV) {
        this.x = x;
        this.y = y;
        this.nbPV = nbPV;
    }

    public void Deplacer() {
        // Return fetch Ilann
    }

    public void Recolter() {
        // Return fetch Ilann

    }

    public void Deposer() {
        // Return fetch Ilann

    }

    public void Attaquer(Planete p) {
       // Return fetch Ilann 
    }

    public void Attaquer(Vaisseau v) {
       // Return fetch Ilann 
    }

    public void Reparer(Vaisseau v) {
        // Return fetch Ilann
    }

    public void Reparer(Planete p) {
        // Return fetch Ilann
    }

    public boolean EstEnOrbite(Vaisseau v) {
        if (carte.getCase(x, y) == Case.Planete) {
            return true;
        }
        return false;
    }

    // Getters et Setters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getNbPV() {
        return nbPV;
    }

    public void setNbPV(int nbPV) {
        this.nbPV = nbPV;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}