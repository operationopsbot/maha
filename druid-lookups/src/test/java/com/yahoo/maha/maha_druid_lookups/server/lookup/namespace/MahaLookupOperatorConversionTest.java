package com.yahoo.maha.maha_druid_lookups.server.lookup.namespace;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.druid.common.config.NullHandling;
import org.apache.druid.jackson.DefaultObjectMapper;
import org.apache.druid.math.expr.ExprMacroTable;
import org.apache.druid.query.lookup.*;
import org.apache.druid.segment.column.RowSignature;
import org.apache.druid.segment.column.ValueType;
import org.apache.druid.sql.calcite.expression.DruidExpression;
import org.apache.druid.sql.calcite.planner.PlannerContext;

import org.easymock.EasyMock;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class MahaLookupOperatorConversionTest {
    MahaLookupTestUtil util = new MahaLookupTestUtil();

    @BeforeTest
    public void setUp(){
        NullHandling.initializeForTests();
    }

    @AfterTest
    public void shutDown() {

    }

    @Test
    public void testLookupReturnsExpectedResults() throws JsonProcessingException {
        RexBuilder rexBuilder = new RexBuilder(util.typeFactory);
        RowSignature ROW_SIGNATURE = RowSignature
                .builder()
                .add("d", ValueType.DOUBLE)
                .add("l", ValueType.LONG)
                .add("s", ValueType.STRING)
                .add("student_id", ValueType.STRING)
                .build();

        final LookupExtractorFactoryContainerProvider manager = EasyMock.createStrictMock(LookupReferencesManager.class);

        MahaLookupOperatorConversion opConversion = new MahaLookupOperatorConversion(manager);
        ExprMacroTable exprMacroTable = TestExprMacroTable.INSTANCE;

        PlannerContext plannerContext = EasyMock.createStrictMock(PlannerContext.class);
        EasyMock.expect(plannerContext.getExprMacroTable()).andReturn(exprMacroTable).anyTimes();
        EasyMock.replay(plannerContext);
        MahaLookupTestUtil.MAHA_LOOKUP mahaLookup = new MahaLookupTestUtil.MAHA_LOOKUP(
                util.makeInputRef("student_id", ROW_SIGNATURE, rexBuilder)
                , Pair.of("student_lookup", SqlTypeName.VARCHAR)
                , Pair.of("student_id", SqlTypeName.VARCHAR)
                , Pair.of("123", SqlTypeName.VARCHAR)
                , rexBuilder
        );

        RexNode rn2 = mahaLookup.makeCall(rexBuilder, opConversion.calciteOperator());

        DruidExpression druidExp = opConversion.toDruidExpression(plannerContext, ROW_SIGNATURE, rn2);
        assert druidExp != null;
        DefaultObjectMapper mapper = new DefaultObjectMapper();

        String expectedDruidExpr = "DruidExpression{simpleExtraction=MahaRegisteredLookupExtractionFn{delegate=null, lookup='student_lookup', retainMissingValue=false, replaceMissingValueWith='123', injective=false, optimize=false, valueColumn=student_id, decodeConfig=null, useQueryLevelCache=false}(student_id), expression='maha'}";
        String json = util.convertToJson(druidExp, "testing_stats", "Student ID");
        assert druidExp.toString().equals(expectedDruidExpr);
        assert json.contains("\"dimensions\":[{\"type\":\"extraction\",\"dimension\":\"student_id\",\"outputName\":\"Student ID\",\"outputType\":\"STRING\",\"extractionFn\":{\"type\":\"mahaRegisteredLookup\",\"lookup\":\"student_lookup\",\"retainMissingValue\":false,\"replaceMissingValueWith\":\"123\",\"injective\":false,\"optimize\":false,\"valueColumn\":\"student_id\",\"decode\":null,\"dimensionOverrideMap\":null,\"useQueryLevelCache\":false}}]");
    }


}
