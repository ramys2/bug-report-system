import RegistrationForm from "../components/RegistrationForm";

/** Guest-only page at `/register`, matching the existing login page layout. */
export default function RegistrationPage() {
    return (
        <main className="container d-flex flex-grow-1 align-items-center justify-content-center py-5">
            <div className="col-12 col-sm-8 col-md-6 col-lg-5">
                <RegistrationForm />
            </div>
        </main>
    );
}
