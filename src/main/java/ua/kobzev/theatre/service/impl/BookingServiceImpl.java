package ua.kobzev.theatre.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ua.kobzev.theatre.domain.AssignedEvent;
import ua.kobzev.theatre.domain.Event;
import ua.kobzev.theatre.domain.Ticket;
import ua.kobzev.theatre.domain.User;
import ua.kobzev.theatre.enums.EventRate;
import ua.kobzev.theatre.repository.TicketRepository;
import ua.kobzev.theatre.service.BookingService;
import ua.kobzev.theatre.service.DiscountService;
import ua.kobzev.theatre.service.EventService;
import ua.kobzev.theatre.util.DomainUtils;
import ua.kobzev.theatre.util.MainUtils;

@Service
public class BookingServiceImpl implements BookingService {

	@Autowired
	private EventService eventService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private DiscountService discountService;

	@Override
	public double getTotalPrice(final Event event, LocalDateTime date, List<Integer> seats, final User user) {
		List<Ticket> ticketsList = new ArrayList<>();

		seats.forEach(seat -> ticketsList.add(createTicket(user, event, date, seat)));

		double price = ticketsList.stream().map(x -> x.getPrice()).reduce((x, y) -> x + y).get();

		double discount = discountService.getDiscount(user, ticketsList);

		return price - discount;
	}

	private Ticket createTicket(User user, Event event, LocalDateTime dateTime, Integer seat) {
		AssignedEvent assignedEvent = eventService.getAssignedEvent(event, dateTime);
		return createTicket(user, assignedEvent, seat);

	}

	private Ticket createTicket(User user, AssignedEvent assignedEvent, int seat) {
		DomainUtils util = MainUtils.getUtils();
		Ticket ticket = util.createNewTicket(user, assignedEvent, seat);

		double baseTicketPrice = assignedEvent.getEvent().getBasePrice();

		EventRate eventRate = assignedEvent.getEvent().getRate();
		if (EventRate.HIGH == eventRate) {
			baseTicketPrice *= 1.2;
		}

		if (assignedEvent.getAuditorium().getVipSeats().contains(seat)) {
			ticket.setPrice(2 * baseTicketPrice);
		} else {
			ticket.setPrice(baseTicketPrice);
		}

		return ticket;
	}

	@Override
	public boolean bookTicket(User user, Ticket ticket) {
		if (ticketRepository.isPurchased(ticket.getAssignedEvent(), ticket.getSeat()))
			return false;

		ticket.setUser(user);
		ticketRepository.save(ticket);

		return true;
	}

	@Override
	public List<Ticket> getTicketsForEvent(Event event, LocalDateTime date) {
		return ticketRepository.findAllByEvent(event, date);
	}

	@Override
	public Ticket createTicket(AssignedEvent assignedEvent, int seat) {
		return createTicket(null, assignedEvent, seat);
	}

}
