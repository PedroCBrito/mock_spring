package br.com.valueprojects.mock_spring.model;

public class EnviadorSMS {

    // Método responsável por enviar SMS ao participante vencedor
    public void enviar(Participante vencedor) {
        // Lógica para envio de SMS (pode ser simulada no teste)
        System.out.println("Enviando SMS para o vencedor: " + vencedor.getNome());
    }
}
