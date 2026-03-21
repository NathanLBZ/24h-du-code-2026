package LesPointeursFous.backend.item;

public class Module {
    private int nbEmplacement;
    private int nbLife;
    private int type;

    public Module(int type, int nbLife, int nbEmplacement){
        this.type = type;
        this.nbLife = nbLife;
        this.nbEmplacement = nbEmplacement;
    }
}
