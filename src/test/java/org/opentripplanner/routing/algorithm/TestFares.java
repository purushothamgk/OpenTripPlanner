/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.SeattleFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

import java.io.File;
import java.util.List;

public class TestFares extends TestCase {

    private AStar aStar = new AStar();
    
    public void testBasic() throws Exception {

        Graph gg = new Graph();
        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(gg);
        gg.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
        RoutingRequest options = new RoutingRequest();
        String feedId = gg.getFeedIds().iterator().next();
        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 12, 0, 0);
        options.dateTime = startTime;
        options.setRoutingContext(gg, feedId + ":Millbrae Caltrain", feedId + ":Mountain View Caltrain");
        ShortestPathTree spt;
        GraphPath path = null;
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(gg.getVertex(feedId + ":Mountain View Caltrain"), true);

        FareService fareService = gg.getService(FareService.class);
        
        Fare cost = fareService.getCost(path);
        assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 425));
    }

    public void testPortland() throws Exception {

        Graph gg = ConstantsForTests.getInstance().getPortlandGraph();
        String feedId = gg.getFeedIds().iterator().next();
        RoutingRequest options = new RoutingRequest();
        ShortestPathTree spt;
        GraphPath path = null;
        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 12, 0, 0);
        options.dateTime = startTime;
        options.setRoutingContext(gg, feedId + ":10579", feedId + ":8371");
        // from zone 3 to zone 2
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(gg.getVertex(feedId + ":8371"), true);
        assertNotNull(path);

        FareService fareService = gg.getService(FareService.class);
        Fare cost = fareService.getCost(path);
        assertEquals(new Money(new WrappedCurrency("USD"), 200), cost.getFare(FareType.regular));

        // long trip

        startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 14, 0, 0);
        options.dateTime = startTime;
        options.setRoutingContext(gg, feedId + ":8389", feedId + ":1252");
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(gg.getVertex(feedId + ":1252"), true);
        assertNotNull(path);
        cost = fareService.getCost(path);
        
        //assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 460));
        
        // complex trip
        options.maxTransfers = 5;
        startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 14, 0, 0);
        options.dateTime = startTime;
        options.setRoutingContext(gg, feedId + ":10428", feedId + ":4231");
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(gg.getVertex(feedId + ":4231"), true);
        assertNotNull(path);
        cost = fareService.getCost(path);
        //
        // this is commented out because portland's fares are, I think, broken in the gtfs. see
        // thread on gtfs-changes.
        // assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 430));
    }
    
    
    public void testKCM() throws Exception {
    	
    	Graph gg = new Graph();
        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.KCM_GTFS));
        
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.setFareServiceFactory(new SeattleFareServiceFactory());
        
        factory.run(gg);
        gg.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
        RoutingRequest options = new RoutingRequest();
        String feedId = gg.getFeedIds().iterator().next();
       
        String vertex0 = feedId + ":2010";
        String vertex1 = feedId + ":2140";
        ShortestPathTree spt;
        GraphPath path = null;
        
        FareService fareService = gg.getService(FareService.class);        
        
        long offPeakStartTime = TestUtils.dateInSeconds("America/Los_Angeles", 2016, 5, 24, 5, 0, 0);
        options.dateTime = offPeakStartTime;
        options.setRoutingContext(gg, vertex0, vertex1);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(vertex1), true);

        Fare costOffPeak = fareService.getCost(path);
        assertEquals(costOffPeak.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 250));
        
        long onPeakStartTime = TestUtils.dateInSeconds("America/Los_Angeles", 2016, 5, 24, 8, 0, 0);
        options.dateTime = onPeakStartTime;
        options.setRoutingContext(gg, vertex0, vertex1);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(vertex1), true);

        Fare costOnPeak = fareService.getCost(path);
        assertEquals(costOnPeak.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 275));
        
    }

    public void testFareComponent() throws Exception {
        Graph gg = new Graph();
        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FARE_COMPONENT_GTFS));
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(gg);
        gg.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
        String feedId = gg.getFeedIds().iterator().next();
        RoutingRequest options = new RoutingRequest();
        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 12, 0, 0);
        options.dateTime = startTime;
        ShortestPathTree spt;
        GraphPath path = null;
        Fare fare = null;
        List<FareComponent> fareComponents = null;
        FareService fareService = gg.getService(FareService.class);
        Money tenUSD = new Money(new WrappedCurrency("USD"), 1000);

        // A -> B, base case
        options.setRoutingContext(gg, feedId+":A", feedId+":B");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":B"), true);
        fare = fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new AgencyAndId(feedId, "AB"));
        assertEquals(fareComponents.get(0).routes.get(0), new AgencyAndId("agency", "1"));

        // D -> E, null case
        options.setRoutingContext(gg, feedId+":D", feedId+":E");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":E"), true);
        fare = fareService.getCost(path);
        assertNull(fare);

        // A -> C, 2 components in a path
        options.setRoutingContext(gg, feedId+":A", feedId+":C");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":C"), true);
        fare = fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 2);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new AgencyAndId(feedId, "AB"));
        assertEquals(fareComponents.get(0).routes.get(0), new AgencyAndId("agency", "1"));
        assertEquals(fareComponents.get(1).price, tenUSD);
        assertEquals(fareComponents.get(1).fareId, new AgencyAndId(feedId, "BC"));
        assertEquals(fareComponents.get(1).routes.get(0), new AgencyAndId("agency", "2"));

        // B -> D, 2 fully connected components
        options.setRoutingContext(gg, feedId+":B", feedId+":D");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":D"), true);
        fare = fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new AgencyAndId(feedId, "BD"));
        assertEquals(fareComponents.get(0).routes.get(0), new AgencyAndId("agency", "2"));
        assertEquals(fareComponents.get(0).routes.get(1), new AgencyAndId("agency", "3"));

        // E -> G, missing in between fare
        options.setRoutingContext(gg, feedId+":E", feedId+":G");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":G"), true);
        fare = fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new AgencyAndId(feedId, "EG"));
        assertEquals(fareComponents.get(0).routes.get(0), new AgencyAndId("agency", "5"));
        assertEquals(fareComponents.get(0).routes.get(1), new AgencyAndId("agency", "6"));

        // C -> E, missing fare after
        options.setRoutingContext(gg, feedId+":C", feedId+":E");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":E"), true);
        fare = fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new AgencyAndId(feedId, "CD"));
        assertEquals(fareComponents.get(0).routes.get(0), new AgencyAndId("agency", "3"));

        // D -> G, missing fare before
        options.setRoutingContext(gg, feedId+":D", feedId+":G");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":G"), true);
        fare = fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new AgencyAndId(feedId, "EG"));
        assertEquals(fareComponents.get(0).routes.get(0), new AgencyAndId("agency", "5"));
        assertEquals(fareComponents.get(0).routes.get(1), new AgencyAndId("agency", "6"));

        // A -> D, use individual component parts
        options.setRoutingContext(gg, feedId+":A", feedId+":D");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(gg.getVertex(feedId+":D"), true);
        fare = fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 2);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new AgencyAndId(feedId, "AB"));
        assertEquals(fareComponents.get(0).routes.get(0), new AgencyAndId("agency", "1"));
        assertEquals(fareComponents.get(1).price, tenUSD);
        assertEquals(fareComponents.get(1).fareId, new AgencyAndId(feedId, "BD"));
        assertEquals(fareComponents.get(1).routes.get(0), new AgencyAndId("agency", "2"));
        assertEquals(fareComponents.get(1).routes.get(1), new AgencyAndId("agency", "3"));
    }
}
