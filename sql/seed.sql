-- Metro Sahaya : sample data (simplified network, SAMPLE fares - not official)
INSERT INTO lines VALUES
 (1,'Purple Line','बैंगनी लाइन','ನೇರಳೆ ಮಾರ್ಗ','#7b1fa2'),
 (2,'Green Line','हरी लाइन','ಹಸಿರು ಮಾರ್ಗ','#2e7d32');

INSERT INTO stations(name, landmark) VALUES
 ('Whitefield','Last stop in the east, IT parks'),
 ('KR Puram','Railway station, bus stop'),
 ('Baiyappanahalli','Bus terminal nearby'),
 ('Indiranagar','100 Feet Road, shops'),
 ('Halasuru','Ulsoor Lake'),
 ('Trinity','Residency Road'),
 ('MG Road','Shopping, Brigade Road, Church Street'),
 ('Cubbon Park','Large park, High Court'),
 ('Vidhana Soudha','State secretariat building'),
 ('Sir M Visvesvaraya','Central College, Visvesvaraya Museum nearby'),
 ('Majestic','Main bus stand (KSRTC) and City Railway Station - change here'),
 ('City Railway Station','Bengaluru City Junction trains'),
 ('Magadi Road','West Bengaluru'),
 ('Vijayanagar','Market area'),
 ('Attiguppe','Residential area'),
 ('Mysore Road','Satellite bus stand nearby'),
 ('Nayandahalli','Outer ring road junction'),
 ('Kengeri','Last stop in the west'),
 ('Nagasandra','Last stop in the north'),
 ('Peenya','Industrial area'),
 ('Yeshwanthpur','Railway station, Orion Mall'),
 ('Mahalakshmi','Mahalakshmi Layout'),
 ('Rajajinagar','Shopping and hospitals'),
 ('Srirampura','North-west residential area'),
 ('Sampige Road','Mantri Square mall, Malleswaram'),
 ('Chickpete','Cloth and silk market'),
 ('KR Market','Big flower and vegetable market'),
 ('National College','Basavanagudi'),
 ('Lalbagh','Lalbagh Botanical Garden'),
 ('South End Circle','Jayanagar area'),
 ('Jayanagar','Shopping complex'),
 ('Banashankari','Banashankari Temple, bus terminal'),
 ('JP Nagar','Residential area'),
 ('Yelachenahalli','South Bengaluru'),
 ('Silk Institute','Last stop in the south');

INSERT INTO line_stations(line_id, seq, station_id)
SELECT v.line_id, v.seq, s.id
FROM (VALUES
 (1, 1,'Whitefield'),(1, 2,'KR Puram'),(1, 3,'Baiyappanahalli'),(1, 4,'Indiranagar'),
 (1, 5,'Halasuru'),(1, 6,'Trinity'),(1, 7,'MG Road'),(1, 8,'Cubbon Park'),
 (1, 9,'Vidhana Soudha'),(1,10,'Sir M Visvesvaraya'),(1,11,'Majestic'),
 (1,12,'City Railway Station'),(1,13,'Magadi Road'),(1,14,'Vijayanagar'),
 (1,15,'Attiguppe'),(1,16,'Mysore Road'),(1,17,'Nayandahalli'),(1,18,'Kengeri'),
 (2, 1,'Nagasandra'),(2, 2,'Peenya'),(2, 3,'Yeshwanthpur'),(2, 4,'Mahalakshmi'),
 (2, 5,'Rajajinagar'),(2, 6,'Srirampura'),(2, 7,'Sampige Road'),(2, 8,'Majestic'),
 (2, 9,'Chickpete'),(2,10,'KR Market'),(2,11,'National College'),(2,12,'Lalbagh'),
 (2,13,'South End Circle'),(2,14,'Jayanagar'),(2,15,'Banashankari'),(2,16,'JP Nagar'),
 (2,17,'Yelachenahalli'),(2,18,'Silk Institute')
) AS v(line_id, seq, sname)
JOIN stations s ON s.name = v.sname;

INSERT INTO fare_slabs VALUES
 (1,2,10),(3,5,20),(6,8,30),(9,11,40),(12,15,50),(16,20,60),(21,999,70);

INSERT INTO guide_steps VALUES
('en',1,'Enter the station','Look for the big "Namma Metro" sign. There is a security check at the entrance: keep your bag open and walk through the scanner.'),
('en',2,'Buy your ticket','Go to the ticket counter or a ticket machine. Say your destination station name, pay the fare shown, and take your token or card.'),
('en',3,'Tap and go in','Hold the token or card on the round reader at the gate. The gate opens. Walk in.'),
('en',4,'Find your platform','Look at the board for the LAST station name in your direction ("towards ..."). Use the stairs or lift to the platform.'),
('en',5,'Board the train','Stand behind the yellow line. Let people get off first, then get in. Listen to the station announcements.'),
('en',6,'Get out','At your station, put the token in the slot of the exit gate. If you have a card, tap it again.'),
('hi',1,'स्टेशन में प्रवेश करें','बड़ा "नम्मा मेट्रो" बोर्ड देखें। प्रवेश पर सुरक्षा जांच होती है: अपना बैग खोलकर रखें और स्कैनर से गुजरें।'),
('hi',2,'टिकट खरीदें','टिकट काउंटर या टिकट मशीन पर जाएं। अपने गंतव्य स्टेशन का नाम बताएं, दिखाया गया किराया दें और टोकन या कार्ड लें।'),
('hi',3,'गेट पर टैप करें','टोकन या कार्ड को गेट के गोल रीडर पर रखें। गेट खुल जाएगा, अंदर जाएं।'),
('hi',4,'सही प्लेटफॉर्म खोजें','बोर्ड पर अपनी दिशा का आखिरी स्टेशन ("... की ओर") देखें। सीढ़ी या लिफ्ट से प्लेटफॉर्म पर जाएं।'),
('hi',5,'ट्रेन में चढ़ें','पीली लाइन के पीछे खड़े रहें। पहले उतरने वालों को उतरने दें, फिर चढ़ें। स्टेशन की घोषणा ध्यान से सुनें।'),
('hi',6,'बाहर निकलें','अपने स्टेशन पर टोकन को निकास गेट के स्लॉट में डालें। कार्ड हो तो फिर से टैप करें।'),
('kn',1,'ನಿಲ್ದಾಣದೊಳಗೆ ಪ್ರವೇಶಿಸಿ','ದೊಡ್ಡ "ನಮ್ಮ ಮೆಟ್ರೋ" ಫಲಕ ನೋಡಿ. ಪ್ರವೇಶದಲ್ಲಿ ಭದ್ರತಾ ತಪಾಸಣೆ ಇರುತ್ತದೆ: ನಿಮ್ಮ ಚೀಲವನ್ನು ತೆರೆದು ಸ್ಕ್ಯಾನರ್ ಮೂಲಕ ಹೋಗಿ.'),
('kn',2,'ಟಿಕೆಟ್ ಖರೀದಿಸಿ','ಟಿಕೆಟ್ ಕೌಂಟರ್ ಅಥವಾ ಟಿಕೆಟ್ ಯಂತ್ರದ ಬಳಿ ಹೋಗಿ. ನಿಮ್ಮ ಗಮ್ಯ ನಿಲ್ದಾಣದ ಹೆಸರು ಹೇಳಿ, ತೋರಿಸಿದ ದರ ಪಾವತಿಸಿ, ಟೋಕನ್ ಅಥವಾ ಕಾರ್ಡ್ ತೆಗೆದುಕೊಳ್ಳಿ.'),
('kn',3,'ಗೇಟ್‌ನಲ್ಲಿ ಟ್ಯಾಪ್ ಮಾಡಿ','ಟೋಕನ್ ಅಥವಾ ಕಾರ್ಡ್ ಅನ್ನು ಗೇಟ್‌ನ ದುಂಡನೆಯ ರೀಡರ್ ಮೇಲೆ ಇಡಿ. ಗೇಟ್ ತೆರೆಯುತ್ತದೆ, ಒಳಗೆ ಹೋಗಿ.'),
('kn',4,'ಸರಿಯಾದ ಪ್ಲಾಟ್‌ಫಾರ್ಮ್ ಹುಡುಕಿ','ಫಲಕದಲ್ಲಿ ನಿಮ್ಮ ದಿಕ್ಕಿನ ಕೊನೆಯ ನಿಲ್ದಾಣದ ಹೆಸರು ನೋಡಿ. ಮೆಟ್ಟಿಲು ಅಥವಾ ಲಿಫ್ಟ್ ಮೂಲಕ ಪ್ಲಾಟ್‌ಫಾರ್ಮ್‌ಗೆ ಹೋಗಿ.'),
('kn',5,'ರೈಲು ಹತ್ತಿ','ಹಳದಿ ಗೆರೆಯ ಹಿಂದೆ ನಿಲ್ಲಿ. ಮೊದಲು ಇಳಿಯುವವರಿಗೆ ದಾರಿ ಬಿಡಿ, ನಂತರ ಹತ್ತಿ. ನಿಲ್ದಾಣದ ಘೋಷಣೆಗಳನ್ನು ಕೇಳಿ.'),
('kn',6,'ಹೊರಬನ್ನಿ','ನಿಮ್ಮ ನಿಲ್ದಾಣದಲ್ಲಿ ಟೋಕನ್ ಅನ್ನು ನಿರ್ಗಮನ ಗೇಟ್‌ನ ಸ್ಲಾಟ್‌ಗೆ ಹಾಕಿ. ಕಾರ್ಡ್ ಇದ್ದರೆ ಮತ್ತೆ ಟ್ಯಾಪ್ ಮಾಡಿ.');
