public class Excecoes {

    public static void main(String[] args) {

        try {

            // NumberFormatException
            // Integer.valueOf("1a");

            // NullPointerException
            String str = null;
            System.out.println(str.length());

        } catch (NumberFormatException e) {
            System.out.println("Erro de conversão de número!");
        } catch (Exception e) {
            System.out.println("Erro!");
        } finally {
            System.out.println("Execução da mesma maneira.");
        }

    }

    private void divisao(int numerador, int denominador) throws Exception {
        if (denominador == 0)
            throw new Exception("Denominador não pode ser zero.");
    }

}