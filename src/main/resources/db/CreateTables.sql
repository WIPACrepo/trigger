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
