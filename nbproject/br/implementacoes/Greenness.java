package implementacoes;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Arrays;
import swing.janelas.PDI_Lote;

/**
 * Classe para os métodos(funcoes) para o calculo do Greenness, onde serão mantidos
 * , que serao usadas pelo resto do programa.
 * 
 * @author Flavia Mattos & Arthur Costa
 */
public class Greenness {

/**
 * Essa função é a implementação da método de GreennesskG = kG − (R + B)
 * onde K é o valor passado pelo usuário e o R,G e B são os valores obtido da imagem
 * 
 * @param img A imagem onde o filtro será aplicado
 * @param sequenciaMin Sequencia minima de descidas pra considerar um vale
 * @return retorna a imagem depois de aplicado o filtro
 */
public BufferedImage GreennKG(BufferedImage img, int sequenciaMin) {
    BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

    //Criação do 
    int histograma[] = new int[256];
    
    for (int i = 0; i < histograma.length; i++) {
        histograma[i] = 0;
    }
    
    for (int i = 0; i < img.getWidth(); i++) {
        for (int j = 0; j < img.getHeight(); j++) {
            Color cor = new Color(img.getRGB(i, j));

            histograma[cor.getRed()]++;
        }
    }
    
    histograma = this.normatizacao(histograma);
            
    histograma = this.filtragemDoHistograma(histograma,11);
    
    int[] minimosLocais = this.minimosLocais(histograma, sequenciaMin);
    System.out.print(Arrays.toString(minimosLocais));
    
    for (int i = 0; i < minimosLocais.length-1; i++) {
        int divisao = minimosLocais[i] + this.deveDividirEm(Arrays.copyOfRange(histograma, minimosLocais[i], minimosLocais[i+1]));
        if (divisao != minimosLocais[i]) {
            int[] novos = new int[minimosLocais.length+1];
            for (int j = 0; j < novos.length; j++) {
                if(divisao == 0)
                    novos[j] = minimosLocais[j-1];
                else if (minimosLocais[j] < divisao) 
                    novos[j] = minimosLocais[j];
                else if (minimosLocais[j] > divisao){
                    novos[j] = divisao;
                    divisao = 0;
                }
            }          
            minimosLocais = novos;
        }
    }
   
    int[] novoHistograma = new int[histograma.length];
    int qtd = 0;
    for (int i = 0; i < minimosLocais.length-1; i++) {
        int[] niveis;
        if(minimosLocais[i+1]==255)
            niveis = this.equalizacao(Arrays.copyOfRange(histograma, minimosLocais[i], minimosLocais[i+1]+1));
        else
            niveis = this.equalizacao(Arrays.copyOfRange(histograma, minimosLocais[i], minimosLocais[i+1]));
        for (int nivel : niveis) {
            novoHistograma[qtd] = minimosLocais[i]+nivel;
            qtd++;
        }
    }
    System.out.print(Arrays.toString(novoHistograma));
    
    for (int i = 0; i < res.getWidth(); i++) {
        for (int j = 0; j < res.getHeight(); j++) {
            Color cor = new Color(img.getRGB(i, j));
            int novoNivel = novoHistograma[cor.getRed()];
            cor = new Color(novoNivel,novoNivel,novoNivel);
            
            res.setRGB(i, j, cor.getRGB());
        }
    }
    return res;
}

/**
 * Avalia e aplica a equalização no histograma dado decidindo os novos níveis
 * 
 * @param histograma
 * @return novos níveis do histograma dado
 */
private int[] equalizacao(int[] histograma){
    double total = 0;
    for (int i = 0; i < histograma.length; i++) {
        total += histograma[i];
    }
    
    double[] somaProbabilidades = new double[histograma.length];
    for (int i = 0; i < histograma.length; i++) {
        somaProbabilidades[i] = 0;
        for (int j = 0; j <= i; j++) {
            somaProbabilidades[i] += ((double)histograma[j])/total;
        }
        somaProbabilidades[i] = somaProbabilidades[i]*(histograma.length-1);
    }
    
    int[] novosNiveis = new int[histograma.length];
    for (int i = 0; i < novosNiveis.length; i++) {
        novosNiveis[i] = Math.round((float)somaProbabilidades[i]);
    }
    
    return novosNiveis;
}

/**
 *  Apresenta o local no qual deve ser dividido o histograma para evitar predominancias
 * 
 * @param intervalo
 * @return posição no intervalo onde deve ocorrer a divisao
 */
private int deveDividirEm(int[] intervalo){
    float media = 0;
    for (int i = 0; i < intervalo.length; i++) {
        media += intervalo[i];
    }
    media = media/intervalo.length;
    
    double variancia = 0;
    for (int i = 0; i < intervalo.length; i++) {
        variancia += Math.pow(intervalo[i]-media, 2);
    }
    variancia = variancia/(intervalo.length-1);
    double desvio = Math.sqrt(variancia);
    
    boolean divisao = false;
    for (int i = 0; i < intervalo.length; i++) 
        if (media+desvio < intervalo[i] || media-desvio > intervalo[i]) {
            divisao = true;
            break;
        }
         
    int retorno = 0;
    float diferenca = 256;
    if (divisao) {
        for (int i = 0; i < intervalo.length; i++) {
            if (Math.abs(intervalo[i]-media) < diferenca) {
                retorno = i;
                diferenca = Math.abs(intervalo[i]-media);
            }
        }
    }
    
    return retorno;
}

/**
 * Normatiza o histograma para que esteja iniciando na primeira posição e terminando na última
 * 
 * @param histograma
 * @return histograma normatizado
 */
private int[] normatizacao(int[] histograma){
    int[] normatizado = new int[histograma.length];
    int min = 0;
    int max = 255;
    
    //definição da primeira posição que possui valor diferente de zero
    int i = 0;
    while (histograma[i+1]==0 && histograma[i]==0){
        i++;
        min = i;
    }
    
    //definição da ultima posição que possui valor diferente de zero
    i = histograma.length-1;
    while (histograma[i-1]==0 && histograma[i]==0){
        i--;
        max = i;
    }
    
    //normatização
    float novoNivel;
    for (i = min; i < max; i++) {
        novoNivel = (((float)(i-min))/(max-min))*255;
        normatizado[Math.round(novoNivel)] = histograma[i];
    }
    
    return normatizado;
}

/**
 * Filtra o histograma no intuito de apresentá-lo sem grandes serrilhados e mais suave.
 * 
 * @param histograma 
 * @param janela
 * @return histograma filtrado
 */
private int[] filtragemDoHistograma(int[] histograma, int janela){
    int[] filtrado = new int[histograma.length];
    //atribuição das primeiras posições e das ultimas
    for (int i = 0; i < janela; i++) {
        filtrado[i] = histograma[i];
        filtrado[histograma.length-1-i] = histograma[histograma.length-1-i];
    }
    
    //filtragem do histograma
    for (int i = janela; i < histograma.length-janela; i++) {
        int soma = 0;
        for (int j = -janela; j <= janela; j++) {
            soma += histograma[i+janela];
        }
        filtrado[i] = soma/(janela*2+1);
    }
    
    return filtrado;
}

/**
 * Percebe todos os mínimos locais no histograma dado
 * 
 * @param histograma
 * @return minimos locais do histograma
 */
private int[] minimosLocais(int[] histograma, int sequenciaMin){
    ArrayList<Integer> indices = new ArrayList();
    int[] crescimentos = new int[histograma.length];
    
    //Zero para garantir que os intervalos comecem do começo
    indices.add(0);
    //definição dos lugares de crescimento do histograma
    crescimentos[0] = 1;
    for (int i = 1; i < histograma.length; i++) {
        if (histograma[i]>=histograma[i-1]) 
            crescimentos[i] = 1;
        else
            crescimentos[i] = 0;
    }
    
    //definição dos vales do histograma
    int sequenciaDecrescente = 0;
    for (int i = 0; i < crescimentos.length; i++) {
        if (crescimentos[i] == 0) {
            sequenciaDecrescente++;
        }
        else if (sequenciaDecrescente>sequenciaMin) {
            indices.add(i);
            sequenciaDecrescente = 0;
        }
    }
    
    //Valor Max para garantir que os intervalos terminem no final
    indices.add(255);

    //gambiarra pra passar por cima de chatice do java
    int[] retorno = new int[indices.size()];
    for (int i = 0; i < indices.size(); i++)
        retorno[i] = indices.get(i);
    return retorno;
}
    
 /**
 * Essa função é a implementação da método de GreennessMin = G − min(R + B)
 * onde o R,G e B são os valores obtido da imagem
 * 
 * @param img A imagem onde o filtro será aplicado
 * @return retorna a imagem depois de aplicado o filtro
 */
    public BufferedImage GreennMin(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇAO
        double min = 10000;
        double max = 0;
        double menor ;

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color x = new Color(img.getRGB(i, j));
                
                if(x.getRed() < x.getBlue()){
                    menor = x.getRed();
                }else{
                    menor = x.getBlue();
                }
                
                double cor = x.getGreen() - menor;

                if (cor < min) {
                    min = cor;
                }
                if (cor > max) {
                    max = cor;
                }
            }
        }
        //FINAL NORMALIZAÇÃO

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                
                Color p = new Color(img.getRGB(i, j));
                
                if(p.getRed() < p.getBlue()){
                    menor = p.getRed();
                }else {
                    menor = p.getBlue();
                }                
                
                
                double atual = p.getGreen() - menor;
                double cor = 255 * ((atual - min) / (max - min));

                int corB31 = (int) cor;

                Color novo = new Color(corB31, corB31, corB31);
                res.setRGB(i, j, novo.getRGB());
                
            }
        }
        return res;
    }
    
    /**
     * Essa função é a implementação da método de GreennessG−R = (G + R)/(G - R)
     * onde o R,G e B são os valores obtido da imagem
     * @param img A imagem onde o filtro será aplicado
     * @return retorna a imagem depois de aplicado o filtro
     */
    public BufferedImage GreennGmenR(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇÃO
        double min = 10000;
        double max = 0;
        int zero;
        double cor = 0;
        
        try {
            
            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    Color x = new Color(img.getRGB(i, j));

                    zero = x.getGreen() + x.getRed();

                    if(zero != 0){

                        cor = (x.getGreen() - x.getRed()) / ((x.getGreen() + x.getRed())); //Oq fazer se tiver um pixel preto?

                    }else{

                        cor = 0;

                    }

                    if (cor < min) {
                        min = cor;
                    }
                    if (cor > max) {
                        max = cor;
                    }
                }
            }

            
            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    Color p = new Color(img.getRGB(i, j));

                    zero = p.getGreen() + p.getRed();
                    if(zero != 0){

                        double atual = (p.getGreen() - p.getRed()) / ((p.getGreen() + p.getRed()));
                        cor = 255 * ((atual - min) / (max - min));

                    }else{

                        cor = 0;

                    }

                    int corB31 = (int) cor;

                    Color novo = new Color(corB31, corB31, corB31);
                    res.setRGB(i, j, novo.getRGB());
                }
            }
        } catch (java.lang.ArithmeticException e) {
            
            JOptionPane.showMessageDialog(null, "Divisão por Zero", "Error", 0);
            
        }
        //FINAL NORMALIZAÇÃO

        
    return res;
    }
    
    /**
     * Essa função é a implementação da método de GreennessG−R = (G − R)/(G + R)
     * onde K é o valor passado pelo usuário e o R,G e B são os valores obtido da imagem
     * @param img A imagem onde o filtro será aplicado
     * @return retorna a imagem depois de aplicado o filtro
     */
    public BufferedImage GreennGmaisR(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇÃO
        double min = 10000;
        int zero;
        double max = 0;
        double cor = 0;

        try {
            

            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    Color x = new Color(img.getRGB(i, j));

                    zero = x.getGreen() - x.getRed();

                    if(zero != 0){

                        cor = (x.getGreen() + x.getRed() / (x.getGreen() - x.getRed()));

                    }else{

                        cor = 0;

                    }

                        if (cor < min) {
                            min = cor;
                        }
                        if (cor > max) {
                            max = cor;
                        }
                    }
            }
       
        //FINAL NORMALIZAÇÃO

       
            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    Color p = new Color(img.getRGB(i, j));

                    zero = p.getGreen() - p.getRed();

                    if(zero != 0){

                            double atual = (p.getGreen() + p.getRed() / (p.getGreen() - p.getRed()));
                            cor = 255 * ((atual - min) / (max - min));

                    }else{

                        cor = 0;

                    }

                        int corB31 = (int) cor;

                        Color novo = new Color(corB31, corB31, corB31);
                        res.setRGB(i, j, novo.getRGB());
                }
            }
        } catch (Exception e) {
            
            JOptionPane.showMessageDialog(null, "Divisão por Zero", "Error", 0);
            
        }
        
        
    return res;
    }

    /**
     * Essa função é a implementação da método de Greenness = (G)/(R + G + B)
     * onde o R,G e B são os valores obtido da imagem
     * @param img
     * @return 
     */
    public BufferedImage Greenn(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇÃO
        double min = 10000;
        double max = 0;

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color x = new Color(img.getRGB(i, j));

                double cor = x.getGreen() - (x.getRed() + x.getGreen() + x.getBlue());

                if (cor < min) {
                    min = cor;
                }
                if (cor > max) {
                    max = cor;
                }
            }
        }
        //FINAL NORMALIZAÇÃO

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color p = new Color(img.getRGB(i, j));

                double atual = p.getGreen() - (p.getRed() + p.getGreen() + p.getBlue() );

                double cor = 255 * ((atual - min) / (max - min));

                int corB32 = (int) cor;
                Color novo = new Color(corB32, corB32, corB32);
                res.setRGB(i, j, novo.getRGB());
            }
        }
        return res;
    }

    
    /**
     * Essa função é a implementação da método de GreennessSmolka = (G − Max{R,B})^2/G
     * onde o R,G e B são os valores obtido da imagem
     * @param img A imagem onde o filtro será aplicado
     * @return retorna a imagem depois de aplicado o filtro
     */
    public BufferedImage GreennSmolka(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇÃO
        double min = 10000;
        int zero;
        double max = 0;
        double cor = 0;
        double maior;

        try {
            

            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    Color x = new Color(img.getRGB(i, j));

                    zero = x.getGreen();

                    if(zero != 0){

                        if(x.getRed() > x.getBlue()){
                            maior = x.getRed();
                        }else{
                            maior = x.getBlue();
                        }

                        cor = x.getGreen() - Math.pow((maior),9) / x.getGreen();

                    }else{

                        cor = 0;

                    }

                        if (cor < min) {
                            min = cor;
                        }
                        if (cor > max) {
                            max = cor;
                        }
                    }
            }
       
        //FINAL NORMALIZAÇÃO

       
            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    Color p = new Color(img.getRGB(i, j));

                    zero = p.getGreen();

                    if(zero != 0){

                        if(p.getRed() > p.getBlue()){
                            maior = p.getRed();
                        }else{
                            maior = p.getBlue();
                        }

                        double atual = p.getGreen() - Math.pow(( maior),9) / p.getGreen();
                        cor = 255 * ((atual - min) / (max - min));
                        
                    }else{

                        cor = 0;

                    }

                            
                        int corB31 = (int) cor;

                        Color novo = new Color(corB31, corB31, corB31);
                        res.setRGB(i, j, novo.getRGB());
                }
            }
        } catch (Exception e) {
            
            JOptionPane.showMessageDialog(null, "Divisão por Zero", "Error", 0);
            
        }
        
    return res;
    }

    /**
    * Essa função é a implementação da método de GreennessG2 = (G^2 )/(B^2 + R^2 + k)
    * onde K é o valor passado pelo usuário e o R,G e B são os valores obtido da imagem
    * 
    * @param img A imagem onde o filtro será aplicado
    * @param K O valor K da equação
    * @return retorna a imagem depois de aplicado o filtro
    */
    public BufferedImage GreennG2(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇAO
        double min = 10000;
        double max = 0;
        double var = 14;
        double cor = 0;
        double maior;
        double zero;

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                
                Color x = new Color(img.getRGB(i, j));
                
                zero = Math.pow(x.getBlue(),2) + Math.pow(x.getRed(),2) + var;

                if(zero != 0){

                    cor = Math.pow(x.getGreen(),2) / zero;

                }else{

                    cor = 0;

                }

                if (cor < min) {
                    min = cor;
                }
                if (cor > max) {
                    max = cor;
                }
            }
        }
        //FINAL NORMALIZAÇÃO

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {

                Color p = new Color(img.getRGB(i, j));
                
                zero = Math.pow(p.getBlue(),2) + Math.pow(p.getRed(),2) + var;
                
                if(zero != 0){
                    
                    double atual = Math.pow(p.getGreen(),2) / zero;
                    cor = 255 * ((atual - min) / (max - min));

                }else{

                    cor = 0;

                }

                if (cor < min) {
                    min = cor;
                }
                if (cor > max) {
                    max = cor;
                }
                
                int corBK = (int) cor;

                Color novo = new Color(corBK, corBK, corBK);
                res.setRGB(i, j, novo.getRGB());
            }
        }
        return res;
    }
    
    /**
     * Essa função é a implementação da método de GreennessIPCA = I P CA = 0.7582|R − B| − 0.1168|R − G| + 0.6414|G − B|
     * onde o R,G e B são os valores obtido da imagem
     * @param img
     * @return 
     */
    public BufferedImage GreennIPCA(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇÃO
        double min = 10000;
        double max = 0;
        double RB, RG, GB;
        
        

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color x = new Color(img.getRGB(i, j));
                
                RB = x.getRed() - x.getBlue();
                RG = x.getRed() - x.getGreen();
                GB = x.getGreen() - x.getBlue();

                //Colocar a função IPCA
                double cor = 0.7582*(Math.abs(RB)) - 0.1168*(Math.abs(RG)) + 0.6414*(Math.abs(GB));

                if (cor < min) {
                    min = cor;
                }
                if (cor > max) {
                    max = cor;
                }
            }
        }
        //FINAL NORMALIZAÇÃO

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color p = new Color(img.getRGB(i, j));

                RB = p.getRed() - p.getBlue();
                RG = p.getRed() - p.getGreen();
                GB = p.getGreen() - p.getBlue();
                
                double atual = 0.7582*(Math.abs(RB)) - 0.1168*(Math.abs(RG)) + 0.6414*(Math.abs(GB));

                double cor = 255 * ((atual - min) / (max - min));

                int corB32 = (int) cor;
                Color novo = new Color(corB32, corB32, corB32);
                res.setRGB(i, j, novo.getRGB());
            }
        }
        return res;
    }
    
    public BufferedImage BIEspacoX(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇÃO
        double min = 100000;
        double max = 0;

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
               Color x = new Color(img.getRGB(i, j));
               
                //Conversão de RGB para Espaço de cor XYZ
                double Xx = x.getRed() * 0.4124 + x.getGreen() * 0.3575 + x.getBlue() * 0.1804;
                double Yx = x.getRed() * 0.2126 + x.getGreen() * 0.7156 + x.getBlue() * 0.0721;
                double Zx = x.getRed() * 0.0193 + x.getGreen() * 0.1191 + x.getBlue() * 0.9502;

                //Conversão de XYZ para L* a* b*
                double L = (116 * (Yx / 100) - 16);
                double a = 500 * ((Xx / 95.047) - (Yx / 100));
                double b = 200 * ((Yx / 100) - (Zx / 108.883));

                double P = (a + (1.75 * L)) / ((5.465 * L) + a - (3.012 * b));
                double cor = (100 * (P - 0.31)) / 0.17;

                if (cor < min) {
                    min = cor;
                }
                if (cor > max) {
                    max = cor;
                }
            }
        }
        //FINAL NORMALIZAÇÃO

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color p = new Color(img.getRGB(i, j));
                //Conversão de RGB para Espaço de cor XYZ
                double X = p.getRed() * 0.4124 + p.getGreen() * 0.3575 + p.getBlue() * 0.1804;
                double Y = p.getRed() * 0.2126 + p.getGreen() * 0.7156 + p.getBlue() * 0.0721;
                double Z = p.getRed() * 0.0193 + p.getGreen() * 0.1191 + p.getBlue() * 0.9502;

                //Conversão de XYZ para L* a* b*
                double L = (116 * (Y / 100) - 16);
                double a = 500 * ((X / 95.047) - (Y / 100));
                double b = 200 * ((Y / 100) - (Z / 108.883));

                double P = (a + 1.75 * L) / (5.465 * L + a - 3.012 * b);
                double atual = (100 * (P - 0.31)) / 0.17;

                double cor = 255 * ((atual - min) / (max - min));

                int corBX = (int) cor;

                Color novo = new Color(corBX, corBX, corBX);
                res.setRGB(i, j, novo.getRGB());

            }
        }
        return res;
    }

    public BufferedImage BIEspacoI(BufferedImage img) {
        BufferedImage res = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        //COMEÇO NORMALIZAÇÃO
        double min = 100000;
        double max = 0;

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color x = new Color(img.getRGB(i, j));
                
                //Conversão de RGB para Espaço de cor XYZ
                double Xx = x.getRed() * 0.4124 + x.getGreen() * 0.3575 + x.getBlue() * 0.1804;
                double Yx = x.getRed() * 0.2126 + x.getGreen() * 0.7156 + x.getBlue() * 0.0721;
                double Zx = x.getRed() * 0.0193 + x.getGreen() * 0.1191 + x.getBlue() * 0.9502;

                //Conversão de XYZ para L* a* b*
                double L = (116 * (Yx / 1) - 16);

                double cor = 100 - L;

                if (cor < min) {
                    min = cor;
                }
                if (cor > max) {
                    max = cor;
                }
            }
        }
        //FINAL NORMALIZAÇÃO

        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                Color p = new Color(img.getRGB(i, j));

                //Conversão de RGB para Espaço de cor XYZ
                double X = p.getRed() * 0.4124 + p.getGreen() * 0.3575 + p.getBlue() * 0.1804;
                double Y = p.getRed() * 0.2126 + p.getGreen() * 0.7156 + p.getBlue() * 0.0721;
                double Z = p.getRed() * 0.0193 + p.getGreen() * 0.1191 + p.getBlue() * 0.9502;

                //Conversão de XYZ para L* a* b*
                double L = (116 * (Y / 1) - 16);
                double atual = 100 - L;
                double cor = 255 * ((atual - min) / (max - min));

                int corBII = (int) cor;
                
                Color novo = new Color(corBII, corBII, corBII);
                res.setRGB(i, j, novo.getRGB());

            }
        }
        return res;
    }
}
