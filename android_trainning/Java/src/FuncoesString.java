public class FuncoesString {

    public static void main(String[] args) {

        String str = "Curso de Java!";

        System.out.println("Tamanho: " + str.length() + " caracteres.");

        System.out.println("Primeira letra: " + str.charAt(0));

        System.out.println(str.equals("curso")); // false
        System.out.println(str.equals(str)); // true

        System.out.println(str.startsWith("Curso"));

        System.out.println(str.endsWith("!"));

        System.out.println(str.substring(6));

        System.out.println(str.substring(6, 8));

        System.out.println(str.replace("Java!", "Java muito legal!"));

        System.out.println(str.toUpperCase());

        System.out.println(str.toLowerCase());

        System.out.println("     Minha string com espaços em branco       ".trim());

    }
}