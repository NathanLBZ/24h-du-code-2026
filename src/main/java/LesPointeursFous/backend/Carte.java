package LesPointeursFous.backend;

public class Carte {
    private Mappable[][] grille;
    private int xMax = 58;
    private int yMax = 58;

    public Carte(){
        this.grille = new Mappable[xMax][yMax];
        for (int x = 0; x < xMax; x++){
            for (int y = 0; y < yMax; y++){
                this.grille[y][x] = null;
            }
        }
    }

    private boolean checkCase(int x, int y){
        return (x > 0 && x < xMax && y > 0 && y < yMax) ? true : false;
    }

    public Mappable getCase(int x, int y){
        if (checkCase(x, y)){
            return grille[y][x];
        }
        return null;
    }

    public void setCase(int x, int y, Mappable element){
        if (checkCase(x, y)){
            this.grille[y][x] = element;
        }
    }

    public Mappable[][] getVision(int x, int y){
        return null;
    }

    public void call(int x, int y, String parameter){
        this.grille[y][x].call(parameter);
    }
    
}
