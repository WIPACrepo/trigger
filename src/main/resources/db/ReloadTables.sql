DROP TABLE TriggerConfiguration;
DROP TABLE TriggerId;
DROP TABLE TriggerConfig;
DROP TABLE TriggerName;
DROP TABLE ParamConfig;
DROP TABLE Param;
DROP TABLE ParamValue;
DROP TABLE ReadoutConfig;
DROP TABLE ReadoutParameters;

CREATE TABLE ReadoutParameters (
       ReadoutId INTEGER NOT NULL,
       ReadoutType INTEGER,
       TimeOffset INTEGER,
       TimeMinus INTEGER,
       TimePlus INTEGER,
       CONSTRAINT ReadoutParametersPK PRIMARY KEY (ReadoutId)
);

CREATE TABLE ReadoutConfig (
       PrimaryKey INTEGER NOT NULL,
       ReadoutConfigId INTEGER,
       ReadoutId INTEGER,
       CONSTRAINT ReadoutConfigPK PRIMARY KEY (PrimaryKey),
       CONSTRAINT ReadoutParametersFK FOREIGN KEY (ReadoutId) REFERENCES ReadoutParameters (ReadoutId)
);

CREATE TABLE Param (
       ParamId INTEGER NOT NULL,
       ParamName VARCHAR(256),
       CONSTRAINT ParamPK PRIMARY KEY (ParamId)
);

CREATE TABLE ParamValue (
       ParamValueId INTEGER NOT NULL,
       ParamValue VARCHAR(256),
       CONSTRAINT ParamValuePK PRIMARY KEY (ParamValueId)
);

CREATE TABLE ParamConfig (
       PrimaryKey INTEGER NOT NULL,
       ParamConfigId INTEGER,
       ParamId INTEGER,
       ParamValueId INTEGER,
       CONSTRAINT ParamConfigPK PRIMARY KEY (PrimaryKey),
       CONSTRAINT ParamFK FOREIGN KEY (ParamId) REFERENCES Param (ParamId),
       CONSTRAINT ParamValueFK FOREIGN KEY (ParamValueId) REFERENCES ParamValue (ParamValueId)
);

CREATE TABLE TriggerName (
       TriggerType INTEGER NOT NULL,
       TriggerName VARCHAR(256),
       CONSTRAINT TriggerNamePK PRIMARY KEY (TriggerType)
);

CREATE TABLE TriggerConfig (
       TriggerConfigId INTEGER NOT NULL,
       ParamConfigId INTEGER,
       ReadoutConfigId INTEGER,
       CONSTRAINT TriggerConfigPK PRIMARY KEY (TriggerConfigId)
);

CREATE TABLE TriggerId (
       TriggerId INTEGER NOT NULL,
       TriggerType INTEGER,
       TriggerConfigId INTEGER,
       SourceId INTEGER,
       CONSTRAINT TriggerIdPK PRIMARY KEY (TriggerId),
       CONSTRAINT TriggerNameFK FOREIGN KEY (TriggerType) REFERENCES TriggerName (TriggerType),
       CONSTRAINT TriggerConfigFK FOREIGN KEY (TriggerConfigId) REFERENCES TriggerConfig (TriggerConfigId)
);

CREATE TABLE TriggerConfiguration (
       PrimaryKey INTEGER NOT NULL,
       ConfigurationId INTEGER,
       TriggerId INTEGER,
       CONSTRAINT TriggerConfigurationPK PRIMARY KEY (PrimaryKey)
);

INSERT INTO ReadoutParameters VALUES ('-1', '-1', '-1', '-1', '-1');
INSERT INTO ReadoutParameters VALUES ('0', '2', '0', '-8000', '2000');
INSERT INTO ReadoutParameters VALUES ('1', '1', '0', '-8000', '2000');
Insert INTO ReadoutParameters VALUES ('2', '2', '0', '-2000', '2000');
INSERT INTO ReadoutParameters VALUES ('3', '1', '-8000', '-2000', '2000');
INSERT INTO ReadoutParameters VALUES ('4', '2', '0', '-2000', '8000');
INSERT INTO ReadoutParameters VALUES ('5', '1', '0', '-2000', '8000');
INSERT INTO ReadoutParameters VALUES ('6', '2', '8000', '-2000', '2000');
INSERT INTO ReadoutParameters VALUES ('7', '1', '0', '-2000', '2000');
INSERT INTO ReadoutParameters VALUES ('8', '4', '0', '0', '0');
INSERT INTO ReadoutParameters VALUES ('9', '1', '0', '-8000', '8000');
INSERT INTO ReadoutParameters VALUES ('10', '2', '0', '-8000', '8000');
INSERT INTO ReadoutParameters VALUES ('101', '0', '0', '25000', '25000');
INSERT INTO ReadoutParameters VALUES ('102', '0', '0', '10000', '10000');
INSERT INTO ReadoutParameters VALUES ('103', '0', '-6000', '25000', '25000');

INSERT INTO ReadoutConfig VALUES ('0', '-1', '-1');
INSERT INTO ReadoutConfig VALUES ('1', '0', '0');
INSERT INTO ReadoutConfig VALUES ('2', '0', '1');
INSERT INTO ReadoutConfig VALUES ('3', '1', '2');
INSERT INTO ReadoutConfig VALUES ('4', '1', '3');
INSERT INTO ReadoutConfig VALUES ('5', '2', '4');
INSERT INTO ReadoutConfig VALUES ('6', '2', '5');
INSERT INTO ReadoutConfig VALUES ('7', '3', '6');
INSERT INTO ReadoutConfig VALUES ('8', '3', '7');
INSERT INTO ReadoutConfig VALUES ('9', '4', '2');
INSERT INTO ReadoutConfig VALUES ('10', '5', '8');
INSERT INTO ReadoutConfig VALUES ('11', '6', '7');
INSERT INTO ReadoutConfig VALUES ('12', '7', '9');
INSERT INTO ReadoutConfig VALUES ('13', '8', '10');
INSERT INTO ReadoutConfig VALUES ('14', '9', '10');
INSERT INTO ReadoutConfig VALUES ('15', '9', '9');

INSERT INTO ParamValue VALUES ('-1', 'null');
INSERT INTO ParamValue VALUES ('0', '8');
INSERT INTO ParamValue VALUES ('1', '10');
INSERT INTO ParamValue VALUES ('2', '2000');
INSERT INTO ParamValue VALUES ('3', '4');
INSERT INTO ParamValue VALUES ('4', '1');
INSERT INTO ParamValue VALUES ('5', '1000');
INSERT INTO ParamValue VALUES ('6', '2');
INSERT INTO ParamValue VALUES ('7', '6');
INSERT INTO ParamValue VALUES ('8', '7');
INSERT INTO ParamValue VALUES ('9', '100');
INSERT INTO ParamValue VALUES ('10', '5000');

INSERT INTO Param VALUES ('-1', 'null');
INSERT INTO Param VALUES ('0', 'threshold');
INSERT INTO Param VALUES ('1', 'timeWindow');
INSERT INTO Param VALUES ('2', 'hitType');
INSERT INTO Param VALUES ('3', 'prescale');
INSERT INTO Param VALUES ('4', 'triggerType1');
INSERT INTO Param VALUES ('5', 'triggerConfigId1');
INSERT INTO Param VALUES ('6', 'sourceId1');
INSERT INTO Param VALUES ('7', 'triggerType2');
INSERT INTO Param VALUES ('8', 'triggerConfigId2');
INSERT INTO Param VALUES ('9', 'sourceId2');
INSERT INTO Param VALUES ('10', 'triggerType3');
INSERT INTO Param VALUES ('11', 'triggerConfigId3');
INSERT INTO Param VALUES ('12', 'sourceId3');

INSERT INTO ParamConfig VALUES ('0', '-1', '-1', '-1');
INSERT INTO ParamConfig VALUES ('1', '0', '0', '0');
INSERT INTO ParamConfig VALUES ('2', '0', '1', '2');
INSERT INTO ParamConfig VALUES ('3', '1', '0', '1');
INSERT INTO ParamConfig VALUES ('4', '1', '1', '2');
INSERT INTO ParamConfig VALUES ('5', '2', '2', '3');
INSERT INTO ParamConfig VALUES ('6', '3', '2', '4');
INSERT INTO ParamConfig VALUES ('7', '4', '3', '5');
INSERT INTO ParamConfig VALUES ('8', '5', '4', '7');
INSERT INTO ParamConfig VALUES ('9', '5', '5', '6');
INSERT INTO ParamConfig VALUES ('10', '5', '6', '7');
INSERT INTO ParamConfig VALUES ('11', '5', '7', '8');
INSERT INTO ParamConfig VALUES ('12', '5', '8', '6');
INSERT INTO ParamConfig VALUES ('13', '5', '9', '8');
INSERT INTO ParamConfig VALUES ('14', '6', '3', '9');
INSERT INTO ParamConfig VALUES ('15', '7', '0', '7');
INSERT INTO ParamConfig VALUES ('16', '7', '1', '2');
INSERT INTO ParamConfig VALUES ('17', '8', '0', '0');
INSERT INTO ParamConfig VALUES ('18', '8', '1', '10');

INSERT INTO TriggerConfig VALUES ('-1', '-1', '-1');
INSERT INTO TriggerConfig VALUES ('0', '0', '0');
INSERT INTO TriggerConfig VALUES ('1', '0', '1');
INSERT INTO TriggerConfig VALUES ('2', '1', '2');
INSERT INTO TriggerConfig VALUES ('3', '1', '3');
INSERT INTO TriggerConfig VALUES ('4', '2', '4');
INSERT INTO TriggerConfig VALUES ('5', '3', '5');
INSERT INTO TriggerConfig VALUES ('6', '4', '4');
INSERT INTO TriggerConfig VALUES ('7', '4', '6');
INSERT INTO TriggerConfig VALUES ('8', '5', '-1');
INSERT INTO TriggerConfig VALUES ('9', '6', '7');
INSERT INTO TriggerConfig VALUES ('10', '1', '7');
INSERT INTO TriggerConfig VALUES ('11', '6', '8');
INSERT INTO TriggerConfig VALUES ('12', '0', '8');
INSERT INTO TriggerConfig VALUES ('13', '4', '9');
INSERT INTO TriggerConfig VALUES ('14', '7', '9');
INSERT INTO TriggerConfig VALUES ('15', '8', '9');

INSERT INTO TriggerName VALUES ('0', 'SimpleMajorityTrigger');
INSERT INTO TriggerName VALUES ('1', 'CalibrationTrigger');
INSERT INTO TriggerName VALUES ('2', 'MinBiasTrigger');
INSERT INTO TriggerName VALUES ('3', 'ThroughputTrigger');
INSERT INTO TriggerName VALUES ('4', 'TwoCoincidenceTrigger');
INSERT INTO TriggerName VALUES ('5', 'ThreeCoincidenceTrigger');

INSERT INTO TriggerId VALUES ('0', '0', '0', '4000');
INSERT INTO TriggerId VALUES ('1', '0', '1', '4000');
INSERT INTO TriggerId VALUES ('2', '0', '2', '5000');
INSERT INTO TriggerId VALUES ('3', '0', '3', '5000');
INSERT INTO TriggerId VALUES ('4', '1', '4', '4000');
INSERT INTO TriggerId VALUES ('5', '1', '5', '4000');
INSERT INTO TriggerId VALUES ('6', '2', '6', '4000');
INSERT INTO TriggerId VALUES ('7', '2', '7', '5000');
INSERT INTO TriggerId VALUES ('8', '3', '-1', '6000');
INSERT INTO TriggerId VALUES ('9', '4', '8', '6000');
INSERT INTO TriggerId VALUES ('10', '2', '9', '5000');
INSERT INTO TriggerId VALUES ('11', '0', '10', '5000');
INSERT INTO TriggerId VALUES ('12', '2', '11', '4000');
INSERT INTO TriggerId VALUES ('13', '0', '12', '4000');
INSERT INTO TriggerId VALUES ('14', '2', '13', '5000');
INSERT INTO TriggerId VALUES ('15', '0', '14', '5000');
INSERT INTO TriggerId VALUES ('16', '2', '13', '4000');
INSERT INTO TriggerId VALUES ('17', '0', '15', '4000');

INSERT INTO TriggerConfiguration VALUES ('0', '0', '6');
INSERT INTO TriggerConfiguration VALUES ('1', '0', '7');
INSERT INTO TriggerConfiguration VALUES ('2', '0', '8');
INSERT INTO TriggerConfiguration VALUES ('3', '1', '0');
INSERT INTO TriggerConfiguration VALUES ('4', '1', '2');
INSERT INTO TriggerConfiguration VALUES ('5', '2', '4');
INSERT INTO TriggerConfiguration VALUES ('6', '3', '6');
INSERT INTO TriggerConfiguration VALUES ('7', '3', '7');
INSERT INTO TriggerConfiguration VALUES ('8', '3', '9');
INSERT INTO TriggerConfiguration VALUES ('9', '4', '8');
INSERT INTO TriggerConfiguration VALUES ('10', '4', '10');
INSERT INTO TriggerConfiguration VALUES ('11', '4', '11');
INSERT INTO TriggerConfiguration VALUES ('12', '4', '12');
INSERT INTO TriggerConfiguration VALUES ('13', '4', '13');
INSERT INTO TriggerConfiguration VALUES ('14', '5', '8');
INSERT INTO TriggerConfiguration VALUES ('15', '5', '14');
INSERT INTO TriggerConfiguration VALUES ('16', '5', '15');
INSERT INTO TriggerConfiguration VALUES ('17', '5', '16');
INSERT INTO TriggerConfiguration VALUES ('18', '5', '17');
