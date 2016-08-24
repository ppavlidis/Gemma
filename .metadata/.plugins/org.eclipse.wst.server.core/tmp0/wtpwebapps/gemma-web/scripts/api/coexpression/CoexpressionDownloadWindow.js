Ext.namespace( 'Gemma' );

/**
 * 
 * @type {*}
 */
Gemma.CoexpressionDownloadWindow = Ext.extend( Ext.Window, {
   width : 800,
   height : 400,
   layout : 'fit',

   timeToString : function( timeStamp ) {
      // Make minutes double digits.
      var min = (timeStamp.getMinutes() < 10) ? '0' + timeStamp.getMinutes() : timeStamp.getMinutes();
      return timeStamp.getFullYear() + "/" + (timeStamp.getMonth() + 1) + "/" + timeStamp.getDate() + " "
         + timeStamp.getHours() + ":" + min;
   },

   /**
    * @memberOf Gemma.CoexpressionDownloadWindow
    */
   makeHeaderRow : function() {
      var row = [];
      if ( !this.queryGenesOnlyResults ) {
         row.push( "Query Gene", "Query Gene NCBI Id", "Coexpressed Gene", "Coexpressed Gene NCBI Id", "Specificity",
            "Positive Support", "Negative Support", "Datasets tested" );
      } else {
         row.push( "Query Gene", "Query Gene NCBI Id", "Coexpressed Gene", "Coexpressed Gene NCBI Id", "Specificity",
            "Positive Support", "Negative Support" );
      }
      return row.join( '\t' ) + "\n";
   },

   /**
    * 
    * @param number
    * @returns
    */
   decimalPlaceRounder : function( number ) {
      if ( number === null ) {
         return null;
      }
      return Ext.util.Format.round( number, 4 );
   },

   /**
    * 
    * @param coexresult
    * @returns {String}
    */
   makeResultsRow : function( coexresult ) {
      var row = [];
      // query genes only searches(i.e. the viz search) don't populated numTestedIn for performance reasons.
      if ( !this.queryGenesOnlyResults ) {

         row.push( coexresult.queryGene.officialSymbol, coexresult.queryGene.ncbiId,
            coexresult.foundGene.officialSymbol, coexresult.foundGene.ncbiId,
            coexresult.foundGeneNodeDegree > coexresult.queryGeneNodeDegree ? this
               .decimalPlaceRounder( coexresult.foundGeneNodeDegree ) : this
               .decimalPlaceRounder( coexresult.queryGeneNodeDegree ), coexresult.posSupp, coexresult.negSupp,
            coexresult.numTestedIn );
      } else {
         row.push( coexresult.queryGene.officialSymbol, coexresult.queryGene.ncbiId,
            coexresult.foundGene.officialSymbol, coexresult.foundGene.ncbiId,
            coexresult.foundGeneNodeDegree > coexresult.queryGeneNodeDegree ? this
               .decimalPlaceRounder( coexresult.foundGeneNodeDegree ) : this
               .decimalPlaceRounder( coexresult.queryGeneNodeDegree ), coexresult.posSupp, coexresult.negSupp );
      }
      return row.join( '\t' ) + "\n";
   },

   /**
    * 
    * @param data
    */
   convertText : function( data ) {
      var text = '# Generated by Gemma\n' + '# ' + this.timeToString( new Date() ) + '\n' + '# \n' + '# '
         + String.format( Gemma.CITATION_DIRECTIONS, '\n# ' ) + '\n' + '# \n'
         + '# This functionality is currently in beta. The file format may change in the near future. \n'
         + '# Fields are separated by tabs\n' + '# \n';

      text += this.makeHeaderRow();

      // populate node data plus populate edge data
      for (var i = 0; i < data.length; i++) {
         text += this.makeResultsRow( data[i] );
      }

      this.textAreaPanel.setValue( text );
      this.show();
   },

   initComponent : function() {
      Ext.apply( this, {
         tbar : [ {
            ref : 'selectAllButton',
            xtype : 'button',
            text : 'Select All',
            scope : this,
            handler : function() {
               this.textAreaPanel.selectText();
            }
         } ],
         items : [ new Ext.form.TextArea( {
            ref : 'textAreaPanel',
            readOnly : true,
            autoScroll : true,
            wordWrap : false
         } ) ]
      } );

      Gemma.CoexpressionDownloadWindow.superclass.initComponent.call( this );
   },

   onRender : function() {
      Gemma.CoexpressionDownloadWindow.superclass.onRender.apply( this, arguments );
   }
} );
