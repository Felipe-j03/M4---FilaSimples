import java.util.*;

public class GG1KSimulador {
    // ---------------- Gerador Congruente Linear ----------------
    static class LCG {
        long a, c, m, seed, usados, limite;

        public LCG(long a, long c, long m, long seed, long limite) {
            this.a = a;
            this.c = c;
            this.m = m;
            this.seed = seed;
            this.limite = limite;
        }

        public double nextRandom() {
            seed = ((a * seed) + c) % m;
            usados++;
            return (double) seed / m;
        }
    }

    enum Tipo {
        CHEGADA, SAIDA
    }

    static class Evento implements Comparable<Evento> {
        Tipo tipo;
        double tempo;

        Evento(Tipo t, double tempo) {
            this.tipo = t;
            this.tempo = tempo;
        }

        public int compareTo(Evento o) {
            return Double.compare(this.tempo, o.tempo);
        }
    }

    // ---------------- Simulador ----------------
    static class Simulador {
        int K, servidores;
        double clock = 0.0;
        int clientes = 0; // no sistema
        int atendendo = 0; // quantos em serviço
        double[] tempoEstado; // acumula tempo em cada estado
        double ultimoTempo = 0.0;
        int perdas = 0;
        double tempoGlobal = 0.0;
        int eventosProcessados = 0;
        int maxEventos = 100000;

        PriorityQueue<Evento> fila = new PriorityQueue<>();
        LCG rng;

        Simulador(int K, int servidores, LCG rng) {
            this.K = K;
            this.servidores = servidores;
            this.rng = rng;
            this.tempoEstado = new double[K + 1];
        }

        void primeiroEv() {
            // primeira chegada no tempo 2.0
            fila.add(new Evento(Tipo.CHEGADA, 2.0));
        }

        double valClock(double a, double b) {
            return a + (b - a) * rng.nextRandom();
        }

        void iniciaFila() {
            primeiroEv();
            while (!fila.isEmpty() && eventosProcessados < maxEventos) {
                Evento e = fila.poll();
                double dt = e.tempo - clock;
                if (dt < 0)
                    dt = 0;
                tempoEstado[clientes] += dt;
                clock = e.tempo;

                if (e.tipo == Tipo.CHEGADA)
                    chegada();
                else
                    saida();
                eventosProcessados++;
            }
            tempoGlobal = clock;
        }

        void chegada() {
            // agenda próxima chegada
            try {
                double proxima = clock + valClock(2, 5);
                fila.add(new Evento(Tipo.CHEGADA, proxima));
            } catch (RuntimeException ex) {
                // não agenda nova chegada, mas continua
            }

            if (clientes < K) {
                clientes++;
                while (atendendo < servidores && atendendo < clientes) {
                    atendendo++;
                    double fim = clock + valClock(3, 5);
                    fila.add(new Evento(Tipo.SAIDA, fim));
                }
            } else {
                perdas++;
            }
        }

        void saida() {
            clientes--;
            atendendo--;
            while (atendendo < servidores && atendendo < clientes) {
                atendendo++;
                double fim = clock + valClock(3, 5);
                fila.add(new Evento(Tipo.SAIDA, fim));
            }
        }

        void resultadoFinal() {
            double total = 0.0;
            for (double t : tempoEstado)
                total += t;

            System.out.printf("Tempo global: %.2f%n", tempoGlobal);
            System.out.printf("Clientes perdidos: %d%n", perdas);
            System.out.println("Distribuição de probabilidade:");
            for (int i = 0; i < tempoEstado.length; i++) {
                System.out.printf("Estado %d: tempo = %.2f, prob = %.6f%n",
                        i, tempoEstado[i], (tempoEstado[i] / total) * 100);
            }
        }
    }

    // ---------------- MAIN ----------------
    public static void main(String[] args) {
        long M = (long) Math.pow(2, 32);
        long a = 1664525, c = 1013904223, seed = 5;
        long aleatorios = 100000;

        System.out.println("---- Simulação G/G/1/5 ----");
        LCG rng1 = new LCG(a, c, M, seed, aleatorios);
        Simulador sim1 = new Simulador(5, 1, rng1);
        sim1.iniciaFila();
        sim1.resultadoFinal();

        System.out.println("---- Simulação G/G/2/5 ----");
        LCG rng2 = new LCG(a, c, M, seed, aleatorios);
        Simulador sim2 = new Simulador(5, 2, rng2);
        sim2.iniciaFila();
        sim2.resultadoFinal();

    }
}
