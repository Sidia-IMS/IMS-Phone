public class Funcoes {

    private void helloWorld() {
        System.out.println("Olá, mundo!");
    }

    private void hello(String nome) {
        System.out.println("Olá, " + nome + "!");
    }

    private int soma(int n1, int n2) {
        return n1 + n2;
    }

    public static void main(String[] args) {
        Funcoes funcoes = new Funcoes();
        funcoes.helloWorld();
        funcoes.hello("mundo");
        System.out.println(funcoes.soma(2, 2));
    }

}