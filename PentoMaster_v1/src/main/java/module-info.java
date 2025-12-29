module com.example.PentoMaster_v1{
    requires javafx.controls;
    requires javafx.fxml;

    // هذا هو السطر اللي يحل المشكلة ويسمح لك بتشغيل الأصوات
    requires java.desktop;

    opens com.example.projectics108 to javafx.fxml;
    exports com.example.PentoMaster_v1;
}