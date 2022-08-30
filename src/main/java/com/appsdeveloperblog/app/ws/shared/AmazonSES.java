package com.appsdeveloperblog.app.ws.shared;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.appsdeveloperblog.app.ws.shared.dto.UserDto;
import org.springframework.stereotype.Service;

@Service
public class AmazonSES {
    //This address must be verified with Amazon SES.
    final String FROM = "ggunkut@outlook.com";

    //The subject line for the email
    final String SUBJECT = "One last step to complete your registration with PhotoApp";

    //The HTML body for the email
    final String HTMLBODY = "<h1>Please verify your email address</h1>"
            + "<p> Thank you for registering with our mobile app. To complete registration process and be able to log in,"
            + " click on the following link: "
            + "<a href='http://ec2-13-126-247-220.ap-south-1.compute.amazonaws.com:8080/verification-service/email-verification.html?token=$tokenValue'>"
            + "Final step to complete your registration" + "</a><br/><br/>"
            + "Thank you! And we are waiting for you inside!</p>";

    //The email body for recipients with non-HTML email clients.
    //localhost://8080/verification-service/email-verification/
    final String TEXTBODY = "Please verify your email address"
            + "Thank you for registering with our mobile app. To complete registration process and be able to log in,"
            + "open the following URL in your browser window: "
            + "http://ec2-13-126-247-220.ap-south-1.compute.amazonaws.com:8080/verification-service/email-verification.html?token=$tokenValue"
            + "Tank you! And we are waiting for you inside!";

    public void verifyEmail(UserDto userDto) {

        System.setProperty("aws.accessKeyId", "AKIAU5YEY5G4VKM7D5VO");
        System.setProperty("aws.secretKey", "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");

        AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.AP_SOUTH_1)
                .build();

        String htmlBodyWithToken = HTMLBODY.replace("$tokenValue", userDto.getEmailVerificationToken());
        String textBodyWithToken = TEXTBODY.replace("$tokenValue", userDto.getEmailVerificationToken());

        SendEmailRequest request = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(userDto.getEmail()))
                .withMessage(new Message()
                        .withBody(new Body().withHtml(new Content().withCharset("UTF-8").withData(htmlBodyWithToken))
                                .withText(new Content().withCharset("UTF-8").withData(textBodyWithToken)))
                        .withSubject(new Content().withCharset("Utf-8").withData(SUBJECT)))
                .withSource(FROM);

        client.sendEmail(request);

        System.out.println("Email sent!");
    }
}
