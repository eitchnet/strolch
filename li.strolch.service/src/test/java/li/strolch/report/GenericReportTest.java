package li.strolch.report;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import li.strolch.model.StrolchElement;
import li.strolch.model.StrolchRootElement;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.testbase.runtime.RuntimeMock;
import li.strolch.utils.collections.DateRange;
import li.strolch.utils.collections.MapOfSets;
import org.hamcrest.MatcherAssert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GenericReportTest {

	private static final String RUNTIME_PATH = "target/GenericReportTest/"; //$NON-NLS-1$
	private static final String CONFIG_SRC = "src/test/resources/reporttest"; //$NON-NLS-1$

	private static RuntimeMock runtimeMock;
	private static Certificate certificate;

	@BeforeClass
	public static void beforeClass() {
		runtimeMock = new RuntimeMock().mockRuntime(RUNTIME_PATH, CONFIG_SRC);
		runtimeMock.startContainer();
		certificate = runtimeMock.loginTest();
	}

	@AfterClass
	public static void afterClass() {
		runtimeMock.logout(certificate);
		runtimeMock.destroyRuntime();
	}

	@Test
	public void shouldGenerateJsonReport() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			report.doReportAsJson() //
					.forEach(e -> {

						switch (e.get("slot").getAsString()) {
						case "Slot 1":

							assertEquals("Product 01", e.get("product").getAsString());
							assertEquals("20.0", e.get("quantity").getAsString());
							assertEquals("40.0", e.get("maxQuantity").getAsString());
							assertEquals("4.0", e.get("minQuantity").getAsString());
							assertEquals("Section 001", e.get("section").getAsString());
							assertEquals("Storage 01", e.get("storage").getAsString());
							assertEquals("Location 01", e.get("location").getAsString());

							break;

						case "Slot 2":

							assertEquals("Product 02", e.get("product").getAsString());
							assertEquals("18.0", e.get("quantity").getAsString());
							assertEquals("20.0", e.get("maxQuantity").getAsString());
							assertEquals("4.0", e.get("minQuantity").getAsString());
							assertEquals("Section 001", e.get("section").getAsString());
							assertEquals("Storage 01", e.get("storage").getAsString());
							assertEquals("Location 01", e.get("location").getAsString());

							break;

						case "Slot 3":

							assertEquals("Product 01", e.get("product").getAsString());
							assertEquals("11.0", e.get("quantity").getAsString());
							assertEquals("40.0", e.get("maxQuantity").getAsString());
							assertEquals("6.0", e.get("minQuantity").getAsString());
							assertEquals("Section 002", e.get("section").getAsString());
							assertEquals("Storage 02", e.get("storage").getAsString());
							assertEquals("Location 02", e.get("location").getAsString());

							break;

						case "Slot 4":

							assertEquals("Product 02", e.get("product").getAsString());
							assertEquals("16.0", e.get("quantity").getAsString());
							assertEquals("20.0", e.get("maxQuantity").getAsString());
							assertEquals("6.0", e.get("minQuantity").getAsString());
							assertEquals("Section 002", e.get("section").getAsString());
							assertEquals("Storage 02", e.get("storage").getAsString());
							assertEquals("Location 02", e.get("location").getAsString());
							break;
						default:

							fail("Unhandled result element: \n" + e.toString());
							break;
						}
					});
		}
	}

	@Test
	public void shouldFilterReport1() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			report.filter("Product", "product01") //
					.doReportAsJson() //
					.forEach(e -> {

						String slotName = e.get("slot").getAsString();
						switch (slotName) {
						case "Slot 1":
						case "Slot 3":
							break;
						default:
							fail("Unexpected slot name " + slotName + ", should have been filtered!");
							break;
						}
					});
		}
	}

	@Test
	public void shouldFilterReport2() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			report.filter("Product", "product01") //
					.filter("Location", "location02") //
					.doReportAsJson() //
					.forEach(e -> {

						String slotName = e.get("slot").getAsString();
						switch (slotName) {
						case "Slot 3":
							break;
						default:
							fail("Unexpected slot name " + slotName + ", should have been filtered!");
							break;
						}
					});
		}
	}

	@Test
	public void shouldFilterReportByDateRange1() {

		Date from = new Date(LocalDate.of(2016, 1, 1).toEpochDay() * 86400000);

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			Date to = new Date(LocalDate.of(2017, 1, 1).toEpochDay() * 86400000);
			DateRange dateRange = new DateRange().from(from, true).to(to, false);

			// expect no slots as all not in date range
			List<JsonObject> result = report.filter("Product", "product01") //
					.dateRange(dateRange) //
					.doReportAsJson() //
					.collect(toList());
			assertTrue(result.isEmpty());
		}

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			Date to = new Date(LocalDate.of(2017, 3, 1).toEpochDay() * 86400000);
			DateRange dateRange = new DateRange().from(from, true).to(to, false);

			// expect 2 slots, as in date range
			List<JsonObject> result = report.filter("Product", "product01") //
					.dateRange(dateRange) //
					.doReportAsJson() //
					.collect(toList());
			assertEquals(2, result.size());
		}
	}

	@Test
	public void shouldFilterReportByDateRange2() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true)) {

			Date from = new Date(LocalDate.of(2016, 1, 1).toEpochDay() * 86400000);

			// expect no orders as all not in date range
			try (Report report = new Report(tx, "fromStockReport")) {

				Date to = new Date(LocalDate.of(2017, 1, 1).toEpochDay() * 86400000);
				DateRange dateRange = new DateRange().from(from, true).to(to, false);

				List<JsonObject> result = report.filter("Product", "product01").dateRange(dateRange).doReportAsJson()
						.collect(toList());
				assertTrue(result.isEmpty());
			}

			// expect 2 orders, as in date range
			try (Report report = new Report(tx, "fromStockReport")) {

				Date to = new Date(LocalDate.of(2017, 3, 1).toEpochDay() * 86400000);
				DateRange dateRange = new DateRange().from(from, true).to(to, false);

				List<JsonObject> result = report.filter("Product", "product01").dateRange(dateRange).doReportAsJson()
						.collect(toList());
				assertEquals(2, result.size());
			}

			// expect 4 orders, as all in date range
			try (Report report = new Report(tx, "fromStockReport")) {

				Date to = new Date(LocalDate.of(2017, 3, 2).toEpochDay() * 86400000);
				DateRange dateRange = new DateRange().from(from, true).to(to, false);

				List<JsonObject> result = report.filter("Product", "product01", "product02").dateRange(dateRange)
						.doReportAsJson().collect(toList());
				assertEquals(4, result.size());
			}
		}
	}

	@Test
	public void shouldCreateFilterCriteria() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			MapOfSets<String, StrolchRootElement> filterCriteria = report.generateFilterCriteria();

			assertFalse(filterCriteria.containsSet("Location"));
			assertFalse(filterCriteria.containsSet("Slot"));
			MatcherAssert
					.assertThat(filterCriteria.getSet("Product").stream().map(StrolchElement::getId).collect(toSet()),
							containsInAnyOrder("product01", "product02"));
			MatcherAssert
					.assertThat(filterCriteria.getSet("Storage").stream().map(StrolchElement::getId).collect(toSet()),
							containsInAnyOrder("storage01", "storage02"));
			MatcherAssert
					.assertThat(filterCriteria.getSet("Section").stream().map(StrolchElement::getId).collect(toSet()),
							containsInAnyOrder("section001", "section002"));
		}
	}

	@Test
	public void shouldCreateFilterCriteriaFiltered1() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			MapOfSets<String, StrolchRootElement> filterCriteria = report.filter("Product", "product01")
					.generateFilterCriteria();

			// assert sequence of filter criteria is correct
			List<String> types = new ArrayList<>(filterCriteria.keySet());
			assertEquals(3, types.size());
			assertEquals("Product", types.get(0));
			assertEquals("Storage", types.get(1));
			assertEquals("Section", types.get(2));

			assertFalse(filterCriteria.containsSet("Location"));
			assertFalse(filterCriteria.containsSet("Slot"));
			MatcherAssert
					.assertThat(filterCriteria.getSet("Product").stream().map(StrolchElement::getId).collect(toSet()),
							containsInAnyOrder("product01"));
			MatcherAssert
					.assertThat(filterCriteria.getSet("Storage").stream().map(StrolchElement::getId).collect(toSet()),
							containsInAnyOrder("storage01", "storage02"));
			MatcherAssert
					.assertThat(filterCriteria.getSet("Section").stream().map(StrolchElement::getId).collect(toSet()),
							containsInAnyOrder("section001", "section002"));
		}
	}

	@Test
	public void shouldCreateFilterCriteriaFiltered2() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "stockReport")) {

			Date from = new Date(LocalDate.of(2017, 3, 1).toEpochDay() * 86400000);
			Date to = new Date(LocalDate.of(2017, 3, 5).toEpochDay() * 86400000);
			DateRange dateRange = new DateRange().from(from, true).to(to, false);

			MapOfSets<String, StrolchRootElement> filterCriteria = report //
					.dateRange(dateRange) //
					.filter("Product", "product01") //
					.generateFilterCriteria();

			assertTrue(filterCriteria.isEmpty());
		}
	}

	@Test
	public void shouldDoAdditionalJoin() {

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true);
				Report report = new Report(tx, "slotsByOrderUsageReport")) {

			AtomicInteger slotsFound = new AtomicInteger();
			report.doReportAsJson().forEach(e -> {

				switch (e.get("slot").getAsString()) {

				case "Slot 1":
				case "Slot 3": {
					assertEquals("Harry", e.get("firstName").getAsString());
					assertEquals("Barns", e.get("lastName").getAsString());
					slotsFound.getAndIncrement();
					break;
				}

				case "Slot 2":
				case "Slot 4":
				case "Slot 5": {
					assertEquals("Geoffrey", e.get("firstName").getAsString());
					assertEquals("Bobcat", e.get("lastName").getAsString());
					slotsFound.getAndIncrement();
					break;
				}

				case "Slot 6": {
					assertEquals("", e.get("firstName").getAsString());
					slotsFound.getAndIncrement();
					break;
				}

				}
			});

			assertEquals(6, slotsFound.get());
		}
	}
}