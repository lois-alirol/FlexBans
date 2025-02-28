$(document).ready(function () {
    // Highlight Active Type
    const currentType = new URLSearchParams(window.location.search).get('type');
    if (currentType) {
        $(`.nav-item[href="?type=${currentType}"]`).addClass('bg-[#888888]');
    } else {
        $('.nav-item[href="?type=bans"]').addClass('bg-[#888888]');
    }



    // Search Filtering
    let typingTimer;
    const typingDelay = 500;

    $('#searchInput').on('input', function () {
        clearTimeout(typingTimer);

        const input = $(this).val().toLowerCase().trim();
        const url = new URL(window.location);

        if (input) {
            url.searchParams.set('player', input);
        } else {
            url.searchParams.delete('player');
        }

        typingTimer = setTimeout(() => {
            window.location.href = url.toString();
        }, typingDelay);
    });



    // Filter Modal
    const modal = $('#filterModal');
    const closeModal = $('.close');

    $('#filterButton').on('click', function () {
        modal.removeClass('hidden');
    });

    closeModal.on('click', function () {
        modal.addClass('hidden');
    });

    $(window).on('click', function (event) {
        if ($(event.target).is(modal)) {
            modal.addClass('hidden');
        }
    });



    // Apply Filters
    $('input[name="dateFilter"]').on('change', function () {
        $('#exactDate, #beforeDate, #afterDate').prop('disabled', true).val('');
        if (this.value === 'exact') {
            $('#exactDate').prop('disabled', false);
        } else if (this.value === 'before') {
            $('#beforeDate').prop('disabled', false);
        } else if (this.value === 'after') {
            $('#afterDate').prop('disabled', false);
        }
    });

    $('#applySort').on('click', function () {
        const url = new URL(window.location);

        const status = $('#status').val();
        if (status) {
            url.searchParams.set('status', status);
        } else {
            url.searchParams.delete('status');
        }

        const executor = $('#executorInput').val().trim();
        const dateFilter = $('input[name="dateFilter"]:checked').val();
        const exactDate = $('#exactDate').val();
        const beforeDate = $('#beforeDate').val();
        const afterDate = $('#afterDate').val();

        if (executor) {
            url.searchParams.set('executor', executor);
        } else {
            url.searchParams.delete('executor');
        }

        if (dateFilter === 'exact' && exactDate) {
            url.searchParams.set('on', exactDate);
            url.searchParams.delete('before');
            url.searchParams.delete('after');
        } else if (dateFilter === 'before' && beforeDate) {
            url.searchParams.set('before', beforeDate);
            url.searchParams.delete('on');
            url.searchParams.delete('after');
        } else if (dateFilter === 'after' && afterDate) {
            url.searchParams.set('after', afterDate);
            url.searchParams.delete('on');
            url.searchParams.delete('before');
        } else {
            url.searchParams.delete('on');
            url.searchParams.delete('before');
            url.searchParams.delete('after');
        }

        window.location.href = url.toString();

        $('#filterModal').addClass('hidden');
    });


    const urlParams = new URLSearchParams(window.location.search);
    const type = urlParams.get('type');
    if (type === 'kicks') {
        $('#punishmentTable th:nth-child(1), #punishmentTable td:nth-child(1)').hide();
        $('#punishmentTable th:nth-child(6), #punishmentTable td:nth-child(6)').hide();
        $('#punishmentTable th:nth-child(7), #punishmentTable td:nth-child(7)').hide();
    }
});

document.querySelectorAll('input[name="dateFilter"]').forEach(radio => {
    radio.addEventListener('change', function() {
        document.getElementById('exactDate').disabled = this.value !== 'exact';
        document.getElementById('beforeDate').disabled = this.value !== 'before';
        document.getElementById('afterDate').disabled = this.value !== 'after';
    });
});