-- Remove CytogenticLocation


ALTER TABLE CHROMOSOME_FEATURE DROP KEY CHROMOSOME_FEATURE_CYTOGENIC_LOCATION_FKC;
ALTER TABLE CHROMOSOME_FEATURE DROP COLUMN CYTOGENIC_LOCATION_FK;


-- postpone

DROP TABLE IF EXISTS CYTOGENETIC_LOCATION;
