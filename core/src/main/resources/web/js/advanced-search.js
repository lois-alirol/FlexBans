document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('searchPlayerButton').addEventListener('click', function () {
        const playerName = document.getElementById('playerInput').value.trim();
        if (playerName) {
            window.location.href = `/player/${encodeURIComponent(playerName)}`;
        } else {
            alert("Please enter a player name.");
        }
    });

    document.getElementById('searchModeratorButton').addEventListener('click', function () {
        const moderatorName = document.getElementById('moderatorInput').value.trim();
        if (moderatorName) {
            window.location.href = `/moderator/${encodeURIComponent(moderatorName)}`;
        } else {
            alert("Please enter a moderator name.");
        }
    });

    document.getElementById('searchPunishmentButton').addEventListener('click', function () {
        const punishmentID = document.getElementById('punishmentInput').value.trim();
        if (punishmentID) {
            window.location.href = `/details/{{punishment_type}}/${encodeURIComponent(punishmentID)}`;
        } else {
            alert("Please enter a Punishment ID");
        }
    });
});