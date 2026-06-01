package com.diplom.gui;

import com.diplom.gerber.parser.GerberFileParser;
import com.diplom.gui.controller.SvgRenderService;
import com.diplom.gui.service.AuthApiService;
import com.diplom.gui.service.CalculationApiService;
import com.diplom.gui.service.ProducerApiService;
import com.diplom.gui.service.TariffApiService;
import com.diplom.gui.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.diplom")
@EnableJpaRepositories("com.diplom.persistence.repository")
@EntityScan("com.diplom.persistence.entity")
public class GerberCostEstimatorApplication extends Application {
    private static ConfigurableApplicationContext springContext;

    @Override
    public void start(Stage primaryStage) {
        GerberFileParser parser = springContext.getBean(GerberFileParser.class);
        SvgRenderService svgService = springContext.getBean(SvgRenderService.class);
        ProducerApiService producerApiService = springContext.getBean(ProducerApiService.class);
        TariffApiService tariffApiService = springContext.getBean(TariffApiService.class);
        CalculationApiService calculationApiService = springContext.getBean(CalculationApiService.class);
        AuthApiService authApiService = springContext.getBean(AuthApiService.class);
        MainView mainView = new MainView(parser, svgService, producerApiService, authApiService, tariffApiService,
                calculationApiService);
        Scene scene = new Scene(mainView, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Анализатор Gerber-файлов");
        primaryStage.show();
    }

    public static void main(String[] args) {
        springContext = SpringApplication.run(GerberCostEstimatorApplication.class, args);
        launch(args);
    }

    @Override
    public void stop() { springContext.close(); }
}