# noinspection MysqlParsingForFile

START TRANSACTION;

-- Drop status FK constraints
ALTER TABLE ANALYSIS
  DROP FOREIGN KEY FKF19622DCA03372D0;
ALTER TABLE BIBLIOGRAPHIC_REFERENCE
  DROP FOREIGN KEY FK296CFF9DA03372D0;
ALTER TABLE BIO_ASSAY
  DROP FOREIGN KEY FKD1BE0842A03372D0;
ALTER TABLE BIO_MATERIAL
  DROP FOREIGN KEY FK198C0A9EA03372D0;
ALTER TABLE CHARACTERISTIC
  DROP FOREIGN KEY FK93DA659BA03372D0;
ALTER TABLE CHROMOSOME_FEATURE
  DROP FOREIGN KEY FK63A870FA03372D0;
ALTER TABLE CONTACT
  DROP FOREIGN KEY FK6382B000A03372D0;
ALTER TABLE EXPERIMENTAL_DESIGN
  DROP FOREIGN KEY FK1EB718F5A03372D0;
ALTER TABLE EXPERIMENTAL_FACTOR
  DROP FOREIGN KEY FK21E15086A03372D0;
ALTER TABLE EXPRESSION_EXPERIMENT_SET
  DROP FOREIGN KEY FKB268DD87A03372D0;
ALTER TABLE EXTERNAL_DATABASE
  DROP FOREIGN KEY FK555EF36FA03372D0;
ALTER TABLE GENE_SET
  DROP FOREIGN KEY FK9A943018A03372D0;
ALTER TABLE PARAMETERIZABLE
  DROP FOREIGN KEY FK8212CED4A03372D0;
ALTER TABLE PROTOCOL
  DROP FOREIGN KEY FKF3B07E98A03372D0;
ALTER TABLE TREATMENT
  DROP FOREIGN KEY FKA2518858A03372D0;
ALTER TABLE USER_GROUP
  DROP FOREIGN KEY FKC62E00EBA03372D0;

-- Altering PHENOTYPE_ASSOCIATION

-- -- Add last updated property to phenotype assoc.
ALTER TABLE PHENOTYPE_ASSOCIATION
  ADD COLUMN LAST_UPDATED DATETIME DEFAULT NULL;

-- -- Set last updated to value from status
UPDATE PHENOTYPE_ASSOCIATION
SET PHENOTYPE_ASSOCIATION.LAST_UPDATED = (SELECT STATUS.LAST_UPDATE_DATE
                                          FROM STATUS
                                          WHERE STATUS.ID = PHENOTYPE_ASSOCIATION.STATUS_FK);

-- -- drop the status FK constraint and column
ALTER TABLE PHENOTYPE_ASSOCIATION
  DROP FOREIGN KEY FKC2E912AAD052C38A;

-- purge the status table before doing any changes to it

DELETE FROM "STATUS" WHERE id NOT IN (SELECT STATUS_FK FROM ARRAY_DESIGN UNION SELECT STATUS_FK FROM INVESTIGATION);

-- rename columns in AD and Investigation
ALTER TABLE ARRAY_DESIGN
  CHANGE `STATUS_FK` `CURATION_DETAILS_FK` BIGINT(20);
ALTER TABLE INVESTIGATION
  CHANGE `STATUS_FK` `CURATION_DETAILS_FK` BIGINT(20);

-- rename table status
RENAME TABLE
    STATUS TO CURATION_DETAILS;

-- change the table structure
-- -- Information is backed up with the STATUS table
ALTER TABLE CURATION_DETAILS
  DROP `CREATE_DATE`;

-- -- Validation flag is discontinued and was never used consistenly.
-- -- Information is backed up with the STATUS table
ALTER TABLE CURATION_DETAILS
  DROP `VALIDATED`;
ALTER TABLE CURATION_DETAILS
  CHANGE `LAST_UPDATE_DATE` `LAST_UPDATED` DATETIME;

ALTER TABLE CURATION_DETAILS
  ADD COLUMN `NEEDS_ATTENTION` TINYINT(4) NOT NULL DEFAULT 0;
ALTER TABLE CURATION_DETAILS
  ADD COLUMN `NOTE` VARCHAR(255) DEFAULT NULL;

ALTER TABLE CURATION_DETAILS
  ADD COLUMN `TROUBLE_AUDIT_EVENT_FK` BIGINT(20) DEFAULT NULL;
ALTER TABLE CURATION_DETAILS
  ADD FOREIGN KEY trouble_audit_event_fk_1(TROUBLE_AUDIT_EVENT_FK) REFERENCES AUDIT_EVENT (ID);
-- these commands are wrong! Fixed in db.1.24.0
ALTER TABLE CURATION_DETAILS
  ADD COLUMN `ATTENTION_AUDIT_EVENT_FK` BIGINT(20) DEFAULT NULL;
ALTER TABLE CURATION_DETAILS
  ADD FOREIGN KEY attention_audit_event_fk_1(TROUBLE_AUDIT_EVENT_FK) REFERENCES AUDIT_EVENT (ID);

ALTER TABLE CURATION_DETAILS
  ADD COLUMN `NOTE_AUDIT_EVENT_FK` BIGINT(20) DEFAULT NULL;
ALTER TABLE CURATION_DETAILS
  ADD FOREIGN KEY note_audit_event_fk_1(TROUBLE_AUDIT_EVENT_FK) REFERENCES AUDIT_EVENT (ID);

-- commit the transaction
COMMIT;

-- data changes
-- -- rename all ExpressionExperimentImpl class names to ExpressionExperiment
UPDATE INVESTIGATION
SET `class` = "ExpressionExperiment"
WHERE class = 'ExpressionExperimentImpl';

-- rename ok event type
UPDATE AUDIT_EVENT_TYPE
SET `class` = "NotTroubledStatusFlagEvent"
WHERE class = 'OKStatusFlagEventImpl';
-- -- rename trouble event types
UPDATE AUDIT_EVENT_TYPE
SET `class` = "TroubledStatusFlagEvent"
WHERE class = 'TroubleStatusFlagEventImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "TroubledStatusFlagEvent"
WHERE class = 'CoexpressionTroubleImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "TroubledStatusFlagEvent"
WHERE class = 'DifferentialExpressionTroubleImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "TroubledStatusFlagEvent"
WHERE class = 'ExperimentalDesignTroubleImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "TroubledStatusFlagEvent"
WHERE class = 'OutlierSampleTroubleImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "TroubledStatusFlagEvent"
WHERE class = 'SampleLayoutTroubleImpl';
-- -- rename Failed analysis event types
UPDATE AUDIT_EVENT_TYPE
SET `class` = "FailedDifferentialExpressionAnalysisEvent"
WHERE class = 'FailedDifferentialExpressionAnalysisEventImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "FailedLinkAnalysisEvent"
WHERE class = 'FailedLinkAnalysisEventImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "FailedMissingValueAnalysisEvent"
WHERE class = 'FailedMissingValueAnalysisEventImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "FailedPCAAnalysisEvent"
WHERE class = 'FailedPCAAnalysisEventImpl';
UPDATE AUDIT_EVENT_TYPE
SET `class` = "FailedProcessedVectorComputationEvent"
WHERE class = 'FailedProcessedVectorComputationEventImpl';

UPDATE ACLOBJECTIDENTITY
SET OBJECT_CLASS = 'ubic.gemma.model.expression.arrayDesign.ArrayDesign'
WHERE OBJECT_CLASS = 'ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl';
UPDATE ACLOBJECTIDENTITY
SET OBJECT_CLASS = 'ubic.gemma.model.expression.experiment.ExpressionExperiment'
WHERE OBJECT_CLASS = 'ubic.gemma.model.expression.experiment.ExpressionExperimentImpl';

-- -- Validation flag is discontinued and was never used consistently. The information these events contain is discarted
-- -- remove all validation events
START TRANSACTION;

DELETE AUDIT_EVENT FROM AUDIT_EVENT
  INNER JOIN AUDIT_EVENT_TYPE ON AUDIT_EVENT.EVENT_TYPE_FK = AUDIT_EVENT_TYPE.ID
WHERE class IN
      ('ValidatedAnnotationsImpl', 'ValidatedExperimentalDesignImpl', 'ValidatedFlagEventImpl', 'ValidatedQualityControlImpl');
DELETE FROM AUDIT_EVENT_TYPE
WHERE class IN
      ('ValidatedAnnotationsImpl', 'ValidatedExperimentalDesignImpl', 'ValidatedFlagEventImpl', 'ValidatedQualityControlImpl');

COMMIT;
