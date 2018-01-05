package org.freeplane.plugin.collaboration.client.event.children_deprecated;

import static org.assertj.core.api.Assertions.assertThat;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryNode;
import org.freeplane.plugin.collaboration.client.event.TestObjects;
import org.freeplane.plugin.collaboration.client.event.children_deprecated.SpecialNodeTypeProcessor;
import org.freeplane.plugin.collaboration.client.event.children_deprecated.SpecialNodeTypeSet;
import org.freeplane.plugin.collaboration.client.event.children_deprecated.SpecialNodeTypeSet.SpecialNodeType;
import org.junit.Test;

public class SpecialNodeTypeProcessorSpec {
	final private TestObjects testObjects = new TestObjects();
	final private MapModel map = testObjects.map;
	final private NodeModel node = testObjects.parent;
	SpecialNodeTypeProcessor uut = new SpecialNodeTypeProcessor();

	
	@Test
	public void returnsEventClass_SpecialNodeTypeSet() throws Exception {
		assertThat(uut.eventClass()).isEqualTo(SpecialNodeTypeSet.class);
	}
	
	@Test
	public void setsOnlySummaryBeginNodeFlag() throws Exception {
		SpecialNodeTypeSet event = SpecialNodeTypeSet.builder().nodeId(node.getID()).content(SpecialNodeType.SUMMARY_BEGIN).build();

		uut.onUpdate(map, event);
		
		assertThat(SummaryNode.isFirstGroupNode(node)).isTrue();
		assertThat(SummaryNode.isSummaryNode(node)).isFalse();
	}
	@Test
	public void setsSummaryEndNodeFlag() throws Exception {
		SpecialNodeTypeSet event = SpecialNodeTypeSet.builder().nodeId(node.getID()).content(SpecialNodeType.SUMMARY_END).build();
		
		uut.onUpdate(map, event);
		
		assertThat(SummaryNode.isFirstGroupNode(node)).isFalse();
		assertThat(SummaryNode.isSummaryNode(node)).isTrue();
	}
	@Test
	public void setsBothSummaryFlags() throws Exception {
		SpecialNodeTypeSet event = SpecialNodeTypeSet.builder().nodeId(node.getID()).content(SpecialNodeType.SUMMARY_BEGIN_END).build();

		uut.onUpdate(map, event);
		
		assertThat(SummaryNode.isFirstGroupNode(node)).isTrue();
		assertThat(SummaryNode.isSummaryNode(node)).isTrue();
	}
}