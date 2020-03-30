public class Strings {
    public static void main(String[] args) {

        String nome = "Java";

        System.out.println("Letra 1: " + nome.charAt(0));
        System.out.println("Letra 2: " + nome.charAt(1));
        System.out.println("Letra 3: " + nome.charAt(2));
        System.out.println("Letra 4: " + nome.charAt(3));

        String sobrenome = "Rules";
        System.out.println(nome + " " + sobrenome);

        Integer idade = 19;
        System.out.println("Idade: " + idade);

        String intStr = String.valueOf(idade);
        String doubleStr = String.valueOf(12.0);
        String floatStr = String.valueOf(12f);
        String longStr = String.valueOf(12l);

    }
}