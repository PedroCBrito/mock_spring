package br.com.valueprojects.mock_spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.FinalizaJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;
import br.com.valueprojects.mock_spring.model.Resultado;
import infra.JogoDao;
import br.com.valueprojects.mock_spring.model.EnviadorSMS;

public class FinalizaJogoTest {

    private EnviadorSMS enviadorSMSMock = mock(EnviadorSMS.class);

    @Test
    public void deveFinalizarJogosDaSemanaAnterior() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Jogo jogo1 = new CriadorDeJogo().para("Cata moedas")
            .naData(antiga).constroi();
        Jogo jogo2 = new CriadorDeJogo().para("Derruba barreiras")
            .naData(antiga).constroi();

        // Adiciona resultados aos jogos
        Participante participante1 = new Participante(1, "Jogador 1");
        jogo1.anota(new Resultado(participante1, 10.0));
        jogo2.anota(new Resultado(participante1, 8.0));

        List<Jogo> jogosAnteriores = Arrays.asList(jogo1, jogo2);
        JogoDao daoFalso = mock(JogoDao.class);
        when(daoFalso.emAndamento()).thenReturn(jogosAnteriores);

        FinalizaJogo finalizador = new FinalizaJogo(daoFalso, enviadorSMSMock);
        finalizador.finaliza();

        assertTrue(jogo1.isFinalizado());
        assertTrue(jogo2.isFinalizado());
        assertEquals(2, finalizador.getTotalFinalizados());
    }

    @Test
    public void deveVerificarSeMetodoAtualizaFoiInvocado() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Jogo jogo1 = new CriadorDeJogo().para("Cata moedas").naData(antiga).constroi();
        Jogo jogo2 = new CriadorDeJogo().para("Derruba barreiras").naData(antiga).constroi();

        // Adiciona resultados aos jogos
        Participante participante1 = new Participante(1, "Jogador 1");
        jogo1.anota(new Resultado(participante1, 10.0));
        jogo2.anota(new Resultado(participante1, 8.0));

        List<Jogo> jogosAnteriores = Arrays.asList(jogo1, jogo2);
        JogoDao daoFalso = mock(JogoDao.class);
        when(daoFalso.emAndamento()).thenReturn(jogosAnteriores);

        FinalizaJogo finalizador = new FinalizaJogo(daoFalso, enviadorSMSMock);
        finalizador.finaliza();

        verify(daoFalso, times(1)).atualiza(jogo1);
        verify(daoFalso, times(1)).atualiza(jogo2);
    }

    // Novos testes

    @Test
    public void deveFinalizarJogosDaSemanaAnteriorSalvarNoBancoEEnviarSMS() {
        Jogo jogo1 = new Jogo("Jogo da Semana Anterior", umaDataDeUmaSemanaAtras());

        // Adiciona resultado para ter um vencedor
        Participante participante1 = new Participante(1, "Jogador 1");
        jogo1.anota(new Resultado(participante1, 9.0));

        JogoDao daoFalso = mock(JogoDao.class);
        when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo1));

        FinalizaJogo finalizador = new FinalizaJogo(daoFalso, enviadorSMSMock);
        finalizador.finaliza();

        // Verifica que o jogo foi finalizado e salvo
        assertTrue(jogo1.isFinalizado());
        verify(daoFalso).atualiza(jogo1);

        // Verifica que o SMS foi enviado após o salvamento
        verify(enviadorSMSMock).enviar(vencedorDoJogo(jogo1));
    }

    @Test
    public void naoDeveEnviarSMSSeNaoSalvarJogo() {
        Jogo jogo1 = new Jogo("Jogo da Semana Anterior", umaDataDeUmaSemanaAtras());

        // Adiciona resultado para ter um vencedor
        Participante participante1 = new Participante(1, "Jogador 1");
        jogo1.anota(new Resultado(participante1, 9.0));

        JogoDao daoFalso = mock(JogoDao.class);
        when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo1));

        // Simula falha no salvamento
        doThrow(new RuntimeException("Erro ao salvar")).when(daoFalso).atualiza(jogo1);

        FinalizaJogo finalizador = new FinalizaJogo(daoFalso, enviadorSMSMock);

        try {
            finalizador.finaliza();
        } catch (Exception e) {
            // Ignora exceção para fins de teste
        }

        // Verifica que o SMS não foi enviado
        verifyNoInteractions(enviadorSMSMock);
    }

    @Test
    public void deveSalvarAntesDeEnviarSMS() {
        Jogo jogo1 = new Jogo("Jogo da Semana Anterior", umaDataDeUmaSemanaAtras());

        // Adiciona resultado para ter um vencedor
        Participante participante1 = new Participante(1, "Jogador 1");
        jogo1.anota(new Resultado(participante1, 9.0));

        JogoDao daoFalso = mock(JogoDao.class);
        when(daoFalso.emAndamento()).thenReturn(Arrays.asList(jogo1));

        FinalizaJogo finalizador = new FinalizaJogo(daoFalso, enviadorSMSMock);
        finalizador.finaliza();

        // Verifica a ordem correta: primeiro salvar, depois enviar SMS
        InOrder inOrder = inOrder(daoFalso, enviadorSMSMock);
        inOrder.verify(daoFalso).atualiza(jogo1);
        inOrder.verify(enviadorSMSMock).enviar(vencedorDoJogo(jogo1));
    }

    private Calendar umaDataDeUmaSemanaAtras() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        return cal;
    }

    private Participante vencedorDoJogo(Jogo jogo) {
        return jogo.getResultados().isEmpty() ? null : jogo.getResultados().get(0).getParticipante();
    }
}
